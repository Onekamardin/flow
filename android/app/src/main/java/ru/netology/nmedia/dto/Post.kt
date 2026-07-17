package ru.netology.nmedia.dto

import ru.netology.nmedia.entity.PostEntity

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likedByMe: Boolean,
    val likes: Int = 0,
)
