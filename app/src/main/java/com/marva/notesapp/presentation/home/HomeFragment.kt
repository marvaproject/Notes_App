package com.marva.notesapp.presentation.home

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.marva.notesapp.data.entity.Notes
import com.marva.notesapp.presentation.NotesViewModel
import com.marva.notesapp.utils.ExtensionFunctions.observeOnce
import com.marva.notesapp.utils.ExtensionFunctions.setupActionBar
import com.marva.notesapp.utils.HelperFunctions
import com.marva.notesapp.utils.HelperFunctions.checkIfDatabaseEmpty
import com.marva.notesapp.R
import com.marva.notesapp.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), SearchView.OnQueryTextListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding as FragmentHomeBinding

    private val homeAdapter by lazy { HomeAdapter() }

    private val homeViewModel by viewModels<NotesViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        binding.mHelperFunctions = HelperFunctions

        setHasOptionsMenu(true)
        binding.toolbarHome.setupActionBar(this, null)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvNotes.apply {
            homeViewModel.getAllData().observe(viewLifecycleOwner) {
                checkIfDatabaseEmpty(it)
                homeAdapter.setData(it)
                scheduleLayoutAnimation()
            }
            adapter = homeAdapter
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)

            swipeToDelete(this)
        }
    }

    private fun swipeToDelete(recyclerView: RecyclerView) {
        val swipeToDeleteCallback = object : SwipeToDelete() {

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val deletedItem = homeAdapter.listNotes[viewHolder.adapterPosition]
                // Delete Item
                homeViewModel.deleteData(deletedItem)
                homeAdapter.notifyItemRemoved(viewHolder.adapterPosition)

                // Restore Deleted Item
                restoreDeletedData(viewHolder.itemView, deletedItem)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    private fun restoreDeletedData(view: View, deletedItem: Notes) {
        val snackBar = Snackbar.make(
            view, "Deleted: '${deletedItem.title}'",
            Snackbar.LENGTH_LONG
        )
        snackBar.setTextColor(ContextCompat.getColor(view.context, R.color.black))
        snackBar.setAction("Undo") {
            homeViewModel.insertData(deletedItem)
        }
        snackBar.show()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_home, menu)

        val search = menu.findItem(R.id.menu_search)
        val searchView = search.actionView as? SearchView
        searchView?.setOnQueryTextListener(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete_all -> deleteAllData()
            R.id.menu_priority_high -> homeViewModel.sortByHighPriority().observe(this) {
                homeAdapter.setData(it)
            }
            R.id.menu_priority_low -> homeViewModel.sortByLowPriority().observe(this) {
                homeAdapter.setData(it)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun deleteAllData() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Everything?")
            .setMessage("Are you sure want to remove everything?")
            .setPositiveButton("Yes") { _, _ ->
                homeViewModel.deleteAllData()
                Toast.makeText(
                    requireContext(),
                    "Successfully Removed Everything",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("No", null)
            .create()
            .show()
    }

    override fun onQueryTextSubmit(query: String?): Boolean {
        if (query != null) {
            searchNote(query)
        }
        return true
    }

    override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null) {
            searchNote(newText)
        }
        return true
    }

    private fun searchNote(query: String) {
        val searchQuery = "%$query%"

        homeViewModel.searchNoteByQuery(searchQuery).observeOnce(this) {
            homeAdapter.setData(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
