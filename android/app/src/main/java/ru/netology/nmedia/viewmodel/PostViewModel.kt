package ru.netology.nmedia.viewmodel

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    content = "",
    author = "",
    authorAvatar = "",
    likedByMe = false,
    likes = 0,
    published = 0,
)

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository =
        PostRepositoryImpl(AppDb.getInstance(context = application).postDao())

    val data: LiveData<FeedModel> = repository.data
        .map(::FeedModel)
        .asLiveData(Dispatchers.Default)

    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState

    private val _placardState = MutableStateFlow(false)
    val placardState: StateFlow<Boolean> = _placardState.asStateFlow()

    private val _scrollToTopEvent = MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)
    val scrollToTopEvent: SharedFlow<Unit> = _scrollToTopEvent.asSharedFlow()

    private val _feedModel = MutableStateFlow(FeedModel(emptyList(), true))
    val feedModel: StateFlow<FeedModel> = _feedModel.asStateFlow()

    init {
        loadPosts()
        viewModelScope.launch {
            repository.data.collect { posts ->
                _feedModel.value = FeedModel(
                    posts = posts,
                    empty = posts.isEmpty()
                )
            }
        }
    }


    private fun checkNewerPosts(feed: FeedModel) = viewModelScope.launch(Dispatchers.IO) {
        val firstId = feed.posts.firstOrNull()?.id ?: 0L
        if (firstId == 0L) {
            _placardState.value = false
            return@launch
        }
        try {
            repository.getNewerCount(firstId).collect { count ->
                _placardState.value = count > 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _placardState.value = false
        }
    }


    private val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                try {
                    repository.save(it)
                    _dataState.value = FeedModelState()
                } catch (e: Exception) {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        edited.value = empty
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) return
        edited.value = edited.value?.copy(content = text)
    }

    fun loadPosts() = viewModelScope.launch {
        _dataState.value = FeedModelState(loading = true)
        try {
            repository.getAll()
            checkNewerPosts(data.value ?: FeedModel(emptyList(), true))
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        } finally {
            _dataState.value = FeedModelState()
        }
    }

    fun refreshPosts() = viewModelScope.launch {
        _dataState.value = FeedModelState(refreshing = true)
        try {
            repository.getAll()
            checkNewerPosts(data.value ?: FeedModel(emptyList(), true))
        } catch (e: Exception) {
            _dataState.value = FeedModelState(error = true)
        } finally {
            _dataState.value = FeedModelState()
        }
    }

    fun likeById(id: Long) {
        TODO("Реализация лайка")
    }

    fun removeById(id: Long) {
        TODO("Реализация удаления")
    }

    fun onNewerPlacardClicked() = viewModelScope.launch {
        _placardState.value = false
        _scrollToTopEvent.tryEmit(Unit)
        repository.markAllNewAsShown()
    }

}
