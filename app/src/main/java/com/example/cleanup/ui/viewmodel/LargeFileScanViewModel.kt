package com.example.cleanup.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cleanup.data.LargeFileInfo
import com.example.cleanup.data.LargeFileManager
import com.example.cleanup.work.EVENT_13
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LargeFileScanViewModel(private val largeFileManager: LargeFileManager) : ViewModel() {
    
    private val _largeFiles = MutableStateFlow<List<LargeFileInfo>>(emptyList())
    val largeFiles: StateFlow<List<LargeFileInfo>> = _largeFiles.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _selectedFiles = MutableStateFlow<Set<String>>(emptySet())
    val selectedFiles: StateFlow<Set<String>> = _selectedFiles.asStateFlow()
    
    private val _sortBy = MutableStateFlow("size")
    val sortBy: StateFlow<String> = _sortBy.asStateFlow()
    
    private val _filteredFiles = MutableStateFlow<List<LargeFileInfo>>(emptyList())
    val filteredFiles: StateFlow<List<LargeFileInfo>> = _filteredFiles.asStateFlow()
    
    init {
        loadLargeFiles()
    }
    
    /**
     * 加载大文件列表
     */
    fun loadLargeFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val files = largeFileManager.scanLargeFiles()
                _largeFiles.value = files
                updateFilteredFiles()
            } catch (e: Exception) {
                println("加载大文件列表异常: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 更新过滤后的文件列表
     */
    private fun updateFilteredFiles() {
        val files = _largeFiles.value
        val sort = _sortBy.value
        
        val sorted = files.sortedWith { a, b ->
            when (sort) {
                "size" -> b.size.compareTo(a.size)
                "name" -> a.name.compareTo(b.name)
                "date" -> b.lastModified.compareTo(a.lastModified)
                else -> 0
            }
        }
        
        _filteredFiles.value = sorted
    }
    
    /**
     * 设置排序方式
     */
    fun setSortBy(sortType: String) {
        EVENT_13(extra = sortType)
        _sortBy.value = sortType
        updateFilteredFiles()
    }
    
    /**
     * 切换文件选择状态
     */
    fun toggleFileSelection(fileName: String) {
        val current = _selectedFiles.value
        _selectedFiles.value = if (current.contains(fileName)) {
            current - fileName
        } else {
            current + fileName
        }
    }
    
    /**
     * 选择所有文件
     */
    fun selectAllFiles() {
        _selectedFiles.value = _largeFiles.value.map { it.name }.toSet()
    }
    
    /**
     * 取消选择所有文件
     */
    fun clearSelection() {
        _selectedFiles.value = emptySet()
    }
    
    /**
     * 删除单个文件
     */
    fun deleteFile(filePath: String) {
        viewModelScope.launch {
            try {
                val success = largeFileManager.deleteFile(filePath)
                if (success) {
                    // 从列表中移除已删除的文件
                    val currentFiles = _largeFiles.value
                    _largeFiles.value = currentFiles.filter { it.path != filePath }
                    updateFilteredFiles()
                    
                    // 从选择列表中移除
                    val fileName = filePath.substringAfterLast('/')
                    val currentSelected = _selectedFiles.value
                    _selectedFiles.value = currentSelected - fileName
                }
            } catch (e: Exception) {
                println("删除文件异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 批量删除选中的文件
     */
    fun deleteSelectedFiles() {
        viewModelScope.launch {
            try {
                val selectedFileNames = _selectedFiles.value
                val selectedFilePaths = _largeFiles.value
                    .filter { it.name in selectedFileNames }
                    .map { it.path }
                
                if (selectedFilePaths.isNotEmpty()) {
                    val deletedFiles = largeFileManager.deleteFiles(selectedFilePaths)
                    
                    // 从列表中移除已删除的文件
                    val currentFiles = _largeFiles.value
                    _largeFiles.value = currentFiles.filter { it.path !in deletedFiles }
                    updateFilteredFiles()
                    
                    // 清空选择
                    _selectedFiles.value = emptySet()
                }
            } catch (e: Exception) {
                println("批量删除文件异常: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): LargeFileStats {
        val files = _largeFiles.value
        val selectedFileNames = _selectedFiles.value
        val selectedFiles = files.filter { it.name in selectedFileNames }
        
        return LargeFileStats(
            totalFiles = files.size,
            totalSize = files.sumOf { it.size },
            selectedFiles = selectedFiles.size,
            selectedSize = selectedFiles.sumOf { it.size }
        )
    }

    fun getSelectSize(): Long{
        return _largeFiles.value.filter { it.name in _selectedFiles.value }.sumOf { it.size }
    }
}

data class LargeFileStats(
    val totalFiles: Int,
    val totalSize: Long,
    val selectedFiles: Int,
    val selectedSize: Long
)

