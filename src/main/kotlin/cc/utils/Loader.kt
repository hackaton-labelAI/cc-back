package cc.utils

import java.io.File

object Loader {

    fun listFilesRecursive(path: String): List<String> {
        val af = File(path).absoluteFile
        val prefixToRemove = af.absolutePath

        fun listFilesRecursive0(
            folder: File
        ): List<String> {
            val files = folder.listFiles().filter { it.isFile }.map { it.absolutePath.removePrefix(prefixToRemove) }
            val folders = folder.listFiles().filter { it.isDirectory }.flatMap { listFilesRecursive0(it) }
            return files + folders
        }
        return listFilesRecursive0(af)
    }

    private val re = Regex(" ([a-z]|\\d){32}")
    fun removeNotionSalt(f0: String): String {
        var f = f0
        while (true) {
            val mr = re.find(f)
            if (mr == null) return f
            f = f.removeRange(mr.groups[0]!!.range.start, mr.groups[0]!!.range.endInclusive + 1)
        }
    }

}