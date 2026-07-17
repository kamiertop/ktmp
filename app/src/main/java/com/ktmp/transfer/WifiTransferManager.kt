package com.ktmp.transfer

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import com.ktmp.data.repository.PlaylistRepository
import com.ktmp.data.scanner.MediaScanner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton

data class TransferState(
    val isRunning: Boolean = false,
    val url: String = "",
    val uploadedCount: Int = 0,
    val status: String = "未启动",
    val clientConnected: Boolean = false
)

@Singleton
class WifiTransferManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaScanner: MediaScanner,
    private val playlistRepository: PlaylistRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var serverSocket: ServerSocket? = null

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    fun start(port: Int = 8080) {
        if (_state.value.isRunning) return
        scope.launch {
            try {
                val ip = getWifiIpAddress() ?: "127.0.0.1"
                serverSocket = ServerSocket(port)
                _state.value = TransferState(
                    isRunning = true,
                    url = "http://$ip:$port",
                    status = "等待连接..."
                )
                while (true) {
                    val client = serverSocket?.accept() ?: break
                    scope.launch { handleClient(client) }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isRunning = false,
                    status = "启动失败: ${e.message}"
                )
            }
        }
    }

    fun stop() {
        try {
            serverSocket?.close()
        } catch (_: Exception) {}
        serverSocket = null
        _state.value = TransferState()
    }

    private suspend fun handleClient(socket: java.net.Socket) {
        // Mark as connected as soon as a browser reaches us (page load or upload)
        _state.value = _state.value.copy(clientConnected = true, status = "已连接")
        try {
            val input = java.io.BufferedInputStream(socket.getInputStream())
            val output = socket.getOutputStream()
            val request = readHttpRequest(input)

            val path = request.path.substringBefore('?')
            when {
                path == "/" -> serveHtml(output)
                path == "/playlists" && request.method == "GET" -> servePlaylists(output)
                path == "/playlists" && request.method == "POST" -> {
                    val result = createPlaylist(request.queryParameter("name"))
                    sendJsonBody(output, result)
                }
                path == "/upload" && request.method == "POST" -> {
                    val result = handleUpload(request, input, request.queryParameter("playlistId")?.toLongOrNull())
                    sendJson(output, result)
                }
                else -> send404(output)
            }
        } catch (_: Exception) {
        } finally {
            try { socket.close() } catch (_: Exception) {}
        }
    }

    private fun serveHtml(output: java.io.OutputStream) {
        val html = """
<!DOCTYPE html>
<html lang="zh">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>上传音乐 - ktmp</title>
<style>
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:-apple-system,system-ui,sans-serif;background:#0f0f0f;color:#eee;display:flex;justify-content:center;align-items:center;min-height:100vh}
.card{background:#1a1a1a;border-radius:16px;padding:32px;width:90%;max-width:520px;text-align:center}
h1{font-size:1.4em;margin-bottom:8px}
.sub{color:#888;font-size:.9em;margin-bottom:24px}
.dropzone{border:2px dashed #444;border-radius:12px;padding:48px 16px;cursor:pointer;transition:border-color .2s;margin-bottom:16px}
.dropzone:hover,.dropzone.drag{border-color:#4caf50;background:#1a2a1a}
.dropzone svg{margin-bottom:12px}
.progress{background:#333;border-radius:8px;height:6px;margin-top:16px;overflow:hidden;display:none}
.progress .bar{background:#4caf50;height:100%;width:0;transition:width .1s}
#status{margin-top:12px;color:#aaa;font-size:.85em}
#count{color:#4caf50;font-weight:bold}
input[type=file]{display:none}
</style>
</head>
<body>
<div class="card">
<h1>ktmp · WiFi 传歌</h1>
<p class="sub">拖放文件到此处，或点击选择</p>
<label style="display:block;text-align:left;color:#aaa;font-size:.85em;margin-bottom:8px" for="playlist">导入到合集</label>
<div style="display:flex;gap:8px;margin-bottom:16px"><select id="playlist" style="flex:1;min-width:0;background:#252525;color:#eee;border:1px solid #444;border-radius:8px;padding:12px"></select><button id="newPlaylist" type="button" style="background:#333;color:#eee;border:1px solid #555;border-radius:8px;padding:0 14px">新建</button></div>
<div class="dropzone" id="drop">
<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#4caf50" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
<p>点击选择文件 或 拖放到这里</p>
<p style="color:#666;font-size:.8em;margin-top:4px">支持 mp3, flac, m4a, mp4 等</p>
</div>
<div class="progress" id="prog"><div class="bar" id="bar"></div></div>
<p id="status"></p>
<p id="count" style="display:none"></p>
</div>
<script>
const drop=document.getElementById('drop'),prog=document.getElementById('prog'),
bar=document.getElementById('bar'),status=document.getElementById('status'),
count=document.getElementById('count'),playlist=document.getElementById('playlist');
let total=0;

function loadPlaylists(selectedId){
  return fetch('/playlists').then(r=>r.json()).then(data=>{
  playlist.replaceChildren(...data.playlists.map(p=>{
    const option=document.createElement('option');
    option.value=p.id;option.textContent=p.name;return option;
  }));
  if(selectedId) playlist.value=selectedId;
  });
}
loadPlaylists().catch(()=>{status.textContent='无法加载合集，将导入默认合集';});

document.getElementById('newPlaylist').addEventListener('click',async()=>{
  const name=prompt('新合集名称');
  if(!name || !name.trim()) return;
  try {
    const response=await fetch('/playlists?name='+encodeURIComponent(name.trim()),{method:'POST'});
    const result=await response.json();
    if(!response.ok || !result.ok) throw new Error(result.error || '创建失败');
    await loadPlaylists(String(result.id));
  } catch(error) {
    status.textContent='创建合集失败：'+error.message;
  }
});

drop.addEventListener('click',()=>{
  const i=document.createElement('input');
  i.type='file';i.multiple=true;
  i.accept='audio/*,video/*';
  i.onchange=()=>uploadAll(Array.from(i.files));
  i.click();
});

drop.addEventListener('dragover',e=>{e.preventDefault();drop.classList.add('drag')});
drop.addEventListener('dragleave',()=>drop.classList.remove('drag'));
drop.addEventListener('drop',e=>{
  e.preventDefault();drop.classList.remove('drag');
  uploadAll(Array.from(e.dataTransfer.files));
});

async function uploadAll(files){
  if(!files.length)return;
  prog.style.display='block';count.style.display='block';
  for(let i=0;i<files.length;i++){
    status.textContent='上传中: '+files[i].name;
    const fd=new FormData();fd.append('file',files[i]);
    try {
      const response=await fetch('/upload?playlistId='+encodeURIComponent(playlist.value),{method:'POST',body:fd});
      const result=await response.json();
      if(!response.ok || !result.ok) throw new Error(result.error || '上传失败');
      total+=result.count;
      count.textContent='✓ 已导入 '+total+' 个文件';
    } catch(error) {
      status.textContent='上传失败: '+files[i].name+'（'+error.message+'）';
      return;
    }
    bar.style.width=((i+1)/files.length*100)+'%';
  }
  status.textContent='完成！可继续上传或关闭页面';
    }
</script>
</body>
</html>
        """.trimIndent()

        val response = """
HTTP/1.1 200 OK
Content-Type: text/html; charset=utf-8
Content-Length: ${html.toByteArray().size}
Connection: close

$html
        """.trimIndent()
        output.write(response.toByteArray())
        output.flush()
    }

    private suspend fun servePlaylists(output: java.io.OutputStream) {
        val defaultId = playlistRepository.createPlaylist("默认合集", "自动保存的文件")
        val playlists = playlistRepository.getAllPlaylists().first()
            .ifEmpty { listOf(com.ktmp.domain.model.Playlist(defaultId, "默认合集", createdAt = 0, updatedAt = 0)) }
        val json = playlists.joinToString(prefix = "{\"playlists\":[", postfix = "]}") {
            "{\"id\":${it.id},\"name\":\"${jsonEscape(it.name)}\"}"
        }
        sendJsonBody(output, json)
    }

    private suspend fun createPlaylist(rawName: String?): String {
        val name = rawName?.trim()?.takeIf { it.isNotEmpty() }
            ?: return "{\"ok\":false,\"error\":\"合集名称不能为空\"}"
        if (name.length > 80) {
            return "{\"ok\":false,\"error\":\"合集名称不能超过 80 个字符\"}"
        }
        val id = playlistRepository.createPlaylist(name, "通过 WiFi 传歌创建")
        return "{\"ok\":true,\"id\":$id,\"name\":\"${jsonEscape(name)}\"}"
    }

    private data class HttpRequest(
        val method: String = "GET",
        val path: String = "/",
        val headers: Map<String, String> = emptyMap()
    )

    private fun readHttpRequest(input: InputStream): HttpRequest {
        val line = readLine(input) ?: return HttpRequest()
        val parts = line.split(" ")
        if (parts.size < 3) return HttpRequest()
        val method = parts[0]
        val path = parts[1]

        val headers = mutableMapOf<String, String>()
        while (true) {
            val headerLine = readLine(input) ?: break
            if (headerLine.isEmpty()) break
            val colon = headerLine.indexOf(':')
            if (colon > 0) {
                val key = headerLine.substring(0, colon).trim().lowercase()
                val value = headerLine.substring(colon + 1).trim()
                headers[key] = value
            }
        }
        return HttpRequest(method, path, headers)
    }

    private fun readLine(input: InputStream): String? {
        val sb = StringBuilder()
        while (true) {
            val b = input.read()
            if (b < 0) return if (sb.isEmpty()) null else sb.toString()
            if (b == '\n'.code) break
            if (b != '\r'.code) sb.append(b.toChar())
        }
        return sb.toString()
    }

    private suspend fun handleUpload(
        request: HttpRequest,
        input: InputStream,
        requestedPlaylistId: Long?
    ): Map<String, Any> {
        try {
            val contentType = request.headers["content-type"]
                ?: return mapOf("ok" to false, "error" to "请求缺少 Content-Type")
            val boundary = contentType.substringAfter("boundary=").trim().trim('"')
            if (boundary.isEmpty()) return mapOf("ok" to false, "error" to "请求缺少 multipart 边界")

            val uri = streamMultipartFile(input, boundary)
                ?: return mapOf("ok" to false, "error" to "未找到受支持的音频或视频文件")
            val ids = mediaScanner.addFiles(listOf(uri))
            if (ids.size == 1) {
                val playlistId = requestedPlaylistId
                    ?.takeIf { playlistRepository.getPlaylistById(it) != null }
                    ?: playlistRepository.createPlaylist("默认合集", "自动保存的文件")
                playlistRepository.addItemToPlaylist(playlistId, ids.first())
                _state.value = _state.value.copy(
                    uploadedCount = _state.value.uploadedCount + 1,
                    status = "已导入 1 个文件"
                )
                return mapOf("ok" to true, "count" to 1)
            }
            return mapOf("ok" to false, "error" to "文件已保存，但导入应用媒体库失败")
        } catch (e: Exception) {
            return mapOf("ok" to false, "error" to (e.message ?: "unknown"))
        }
    }

    /**
     * Reads the single FormData file part directly into MediaStore.  In particular, never build
     * a byte array for the HTTP body: a typical lossless track can exceed the app heap limit.
     */
    private fun streamMultipartFile(input: InputStream, boundary: String): Uri? {
        if (readLine(input) != "--$boundary") return null
        var filename: String? = null
        while (true) {
            val line = readLine(input) ?: return null
            if (line.isEmpty()) break
            if (line.startsWith("Content-Disposition:", ignoreCase = true)) {
                filename = line.substringAfter("filename=\"", "").substringBefore("\"")
                    .takeIf { it.isNotBlank() }?.let(::decodeMultipartFilename)
            }
        }
        val name = filename ?: return null
        val pending = createMediaStoreEntry(name) ?: return null
        return try {
            pending.output.use { copyUntilMultipartBoundary(input, boundary, it) }
            publishMediaStoreEntry(pending.uri)
            pending.uri
        } catch (e: Exception) {
            context.contentResolver.delete(pending.uri, null, null)
            throw e
        }
    }

    private data class PendingMedia(val uri: Uri, val output: OutputStream)

    /** Create a pending shared-media item, which remains invisible until the complete stream arrives. */
    private fun createMediaStoreEntry(filename: String): PendingMedia? {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
            filename.substringAfterLast('.', "").lowercase()
        ) ?: when (filename.substringAfterLast('.', "").lowercase()) {
            "flac" -> "audio/flac"
            "m4a" -> "audio/mp4"
            "opus" -> "audio/opus"
            else -> null
        } ?: return null
        if (!mimeType.startsWith("audio/") && !mimeType.startsWith("video/")) return null

        val isAudio = mimeType.startsWith("audio/")
        val collection = if (isAudio) {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sanitizeName(filename))
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES}/ktmp")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return null
        return try {
            PendingMedia(uri, java.io.BufferedOutputStream(resolver.openOutputStream(uri)
                ?: throw IllegalStateException("无法写入系统媒体库")))
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    private fun publishMediaStoreEntry(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.update(uri, ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }, null, null)
        }
    }

    /** Copy a part until its CRLF-prefixed boundary, retaining only a boundary-sized buffer. */
    private fun copyUntilMultipartBoundary(input: InputStream, boundary: String, output: OutputStream) {
        val marker = "\r\n--$boundary".toByteArray(Charsets.UTF_8)
        val prefix = IntArray(marker.size)
        for (i in 1 until marker.size) {
            var j = prefix[i - 1]
            while (j > 0 && marker[i] != marker[j]) j = prefix[j - 1]
            if (marker[i] == marker[j]) j++
            prefix[i] = j
        }
        val pending = ByteArray(marker.size)
        var pendingSize = 0
        var matched = 0
        while (true) {
            val value = input.read()
            if (value < 0) throw IllegalStateException("上传数据不完整")
            val byte = value.toByte()
            pending[pendingSize++] = byte
            while (matched > 0 && byte != marker[matched]) matched = prefix[matched - 1]
            if (byte == marker[matched]) matched++
            if (matched == marker.size) return

            // Keep only the suffix that could still be the start of the marker.
            val writeCount = pendingSize - matched
            if (writeCount > 0) {
                output.write(pending, 0, writeCount)
                System.arraycopy(pending, writeCount, pending, 0, matched)
                pendingSize = matched
            }
        }
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[/\\\\:*?\"<>|]"), "_").ifBlank { "unknown" }
    }

    /** HTTP headers are read byte-for-byte; restore the UTF-8 filename sent by browsers. */
    private fun decodeMultipartFilename(value: String): String =
        value.toByteArray(Charsets.ISO_8859_1).toString(Charsets.UTF_8)

    private fun HttpRequest.queryParameter(name: String): String? =
        path.substringAfter('?', "").split('&').firstOrNull {
            it.substringBefore('=') == name
        }?.substringAfter('=', "")?.let { URLDecoder.decode(it, "UTF-8") }

    private fun jsonEscape(value: String): String = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")

    private fun sendJson(output: java.io.OutputStream, data: Map<String, Any>) {
        val json = data.entries.joinToString(",", "{", "}") { (k, v) ->
            "\"$k\":${if (v is String) "\"$v\"" else v}"
        }
        sendJsonBody(output, json)
    }

    private fun sendJsonBody(output: java.io.OutputStream, json: String) {
        val response = """
HTTP/1.1 200 OK
Content-Type: application/json
Content-Length: ${json.toByteArray().size}
Connection: close

$json
        """.trimIndent()
        output.write(response.toByteArray())
        output.flush()
    }

    private fun send404(output: java.io.OutputStream) {
        val body = "404 Not Found"
        val response = """
HTTP/1.1 404 Not Found
Content-Type: text/plain
Content-Length: ${body.length}
Connection: close

$body
        """.trimIndent()
        output.write(response.toByteArray())
        output.flush()
    }

    private fun getWifiIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (!ni.isUp || ni.isLoopback) continue
                val addresses = ni.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (addr is java.net.Inet4Address && !addr.isLoopbackAddress) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
