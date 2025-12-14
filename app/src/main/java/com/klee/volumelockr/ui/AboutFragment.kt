package com.klee.volumelockr.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.klee.volumelockr.R
import com.mikepenz.aboutlibraries.LibsBuilder

class AboutFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            val libsFragment = LibsBuilder()
                .supportFragment()

            childFragmentManager.beginTransaction()
                .add(R.id.about_libs_container, libsFragment)
                .commitNow()
        }

        view.post {
            val recyclerView = findRecyclerView(view)
            recyclerView?.let { rv ->
                val spanCount = if (resources.configuration.screenWidthDp >= 600) 2 else 1
                if (spanCount > 1) {
                    rv.layoutManager = GridLayoutManager(requireContext(), spanCount)
                }
            }
        }
    }

    private fun findRecyclerView(view: View): RecyclerView? {
        if (view is RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val rv = findRecyclerView(child)
                if (rv != null) return rv
            }
        }
        return null
    }
}
