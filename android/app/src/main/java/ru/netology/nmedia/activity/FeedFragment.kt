package ru.netology.nmedia.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.viewmodel.PostViewModel
import android.util.DisplayMetrics
import androidx.recyclerview.widget.LinearLayoutManager


class FeedFragment : Fragment() {
    private val viewModel: PostViewModel by activityViewModels()
    private var _binding: FragmentFeedBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFeedBinding.inflate(inflater, container, false)
        val rootView = binding.root

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) = viewModel.edit(post)
            override fun onLike(post: Post) = viewModel.likeById(post.id)
            override fun onRemove(post: Post) = viewModel.removeById(post.id)
            override fun onShare(post: Post) {
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(intent, getString(R.string.chooser_share_post)))
            }
        })
        binding.list.adapter = adapter

        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swiperefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.empty
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.placardState.collectLatest { show ->
                binding.newerPlacard.isVisible = show
            }
        }

        binding.btnNewerPlacard.setOnClickListener {
            viewModel.onNewerPlacardClicked()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.scrollToTopEvent.collect {
                val layoutManager = binding.list.layoutManager as? LinearLayoutManager
                    ?: return@collect

                layoutManager.scrollToPosition(0)
            }
        }


        binding.swiperefresh.setOnRefreshListener {
            viewModel.refreshPosts()
        }

        binding.fab.setOnClickListener {
            findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
        }

        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
