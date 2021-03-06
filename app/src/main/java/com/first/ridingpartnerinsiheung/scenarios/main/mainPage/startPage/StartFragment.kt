package com.first.ridingpartnerinsiheung.scenarios.main.mainPage.startPage

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.first.ridingpartnerinsiheung.R
import com.first.ridingpartnerinsiheung.data.MySharedPreferences
import com.first.ridingpartnerinsiheung.data.RidingData
import com.first.ridingpartnerinsiheung.databinding.FragmentStartBinding
import com.first.ridingpartnerinsiheung.extensions.showToast
import com.first.ridingpartnerinsiheung.scenarios.main.mainPage.MainActivity
import com.first.ridingpartnerinsiheung.views.dialog.ChangeGoalDistanceDialog
import com.first.ridingpartnerinsiheung.views.dialog.RecordDialog
import com.google.android.gms.location.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import kotlin.math.round

class StartFragment : Fragment() {

    //viewBinding
    private val viewModel by viewModels<StartViewModel>()
    lateinit var binding : FragmentStartBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = Firebase.auth
    private val user = auth.currentUser!!.uid

    private lateinit var mLastLocation : Location
    private var mFusedLocationProviderClient : FusedLocationProviderClient? = null
    private lateinit var mLocationRequest: LocationRequest

    private val recordList: ArrayList<RecordList> = ArrayList()

        // ?????? ????????? ????????????
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        initBinding()

        // ?????? ????????? ???????????? ?????? GPS ??????
        mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        startLocationUpdate()
        initObserves()

        //????????? ????????? ???????????? mutablestateflow ???????????? ????????? ??? ??????
        viewModel.run {
            weather.onEach {
                when (it!!.sky) {
                    "??????" -> binding.skyTypeImg.setImageResource(R.drawable.icon_whether_sun3)
                    "?????? ??????" -> binding.skyTypeImg.setImageResource(R.drawable.icon_whether_cloud)
                    "??????" -> binding.skyTypeImg.setImageResource(R.drawable.icon_whether_overcast)
                    else -> binding.skyTypeImg.setImageResource(R.drawable.icon_whether_sun3)
                }
                when (it!!.rainType) {
                    "?????? ?????? ??????" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_sun)
                    "???" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_umbrella)
                    "???/???" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_umbrella)
                    "???" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_snow)
                    "?????????" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_umbrella)
                    "????????? ?????????" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_umbrella)
                    "?????????" -> binding.rainTypeImg.setImageResource(R.drawable.icon_whether_snow)
                }

            }.launchIn(viewLifecycleOwner.lifecycleScope)
        }

        var recentRecord: RidingData

        val prefs = MySharedPreferences((activity as MainActivity).applicationContext)

        if (prefs.ridingDateList != "") {
            val ridingDateList = prefs.ridingDateList!!.split(",")

            val recDoc = db.collection(user)
                .document(ridingDateList[0])

            binding.recordDate.text = "(${ridingDateList[0]})"

            recDoc.get().addOnSuccessListener { documentSnapshot ->
                recentRecord = documentSnapshot.toObject<RidingData>()!!

                binding.timerTv.text = recentRecord.timer.let {
                    "${it / 3600} : ${it / 60} : ${it % 60}"
                }
                binding.distanceTv.text = "${round(recentRecord.sumDistance / 10) / 100} km"
                binding.speedTv.text = "${round(recentRecord.averSpeed / 10) / 100} km/h"
                binding.distanceProgressbar.progress = (recentRecord.sumDistance / 100).toInt()
            }

            setList(ridingDateList);
        }

        if(prefs.goalDistance != 0){
            binding.distanceProgressbar.max = prefs.goalDistance*10
            binding.goalDistanceTv.text = "?????? : ${prefs.goalDistance}km"
        }
        return binding.root
    }

    private fun setList(ridingDateList: List<String>){


        ridingDateList.forEach {
            val recDoc = db.collection(user).document(it)

            recDoc.get().addOnSuccessListener { documentSnapshot ->
                val recordDocument = documentSnapshot.toObject<RidingData>()!!

                val timer = recordDocument.timer.let {
                    "${it / 3600} : ${it / 60} : ${it % 60}"
                }
                val distance = (round(recordDocument.sumDistance / 10) / 100).toString()
                val avgSpeed = (round(recordDocument.averSpeed / 10) / 100).toString()

                val record = RecordList(it, distance , avgSpeed, timer)

                recordList.add(record)
            }
        }

        Handler().postDelayed({
            val recordListAdapter = RecordListAdapter(requireContext(), recordList)
            binding.recorListView.adapter = recordListAdapter
        }, 2000)

        initListClickListener()
    }
    private fun initListClickListener(){
        binding.recorListView.isNestedScrollingEnabled = false

        binding.recorListView.setOnItemClickListener { adapterView, view, i, l ->
            val data = recordList[i]
            data.distance
            showDialog(data.distance, data.avgSpeed, data.time)
        }

    }
    private fun showDialog(distance : String, averSpeed : String, timer : String){
        val dialog = RecordDialog(requireContext())
        dialog.start(distance, averSpeed, timer)
    }

    private fun initObserves(){
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            viewModel.event.collect(){ event ->
                when(event){
                    StartViewModel.StartEvent.ShowDialog -> showDialog()
                }
            }
        }
    }
    private fun showDialog(){
        val prefs = MySharedPreferences((activity as MainActivity).applicationContext)

        val dialog = ChangeGoalDistanceDialog(requireContext())
        dialog.start()
        dialog.setOnClickListener(object:ChangeGoalDistanceDialog.DialogOKCLickListener{
            override fun onOKClicked(data: Int) {
                prefs.goalDistance = data
                showToast("??? ???????????? ???????????????")
            }
        })
    }

    private fun initBinding(inflater: LayoutInflater = this.layoutInflater, container: ViewGroup? = null){
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_start, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdate(){
        requirePermissions()

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper()!!)
    }

    private var mLocationCallback = object  : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            mLastLocation = locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    fun onLocationChanged(location: Location){
        mLastLocation = location
        viewModel.changeLocation(mLastLocation.latitude, mLastLocation.longitude)
    }
    private val PERMISSION_CODE = 100

    //  ?????? ??????
    private fun requirePermissions(){
        val permissions=arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
        val isAllPermissionsGranted = permissions.all { //  permissions??? ?????? ?????? ??????
            ActivityCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
        if (isAllPermissionsGranted) {    //  ?????? ????????? ???????????? ?????? ??????
            //permissionGranted()
        } else { //  ????????? ?????? ?????? ?????? ??????
            ActivityCompat.requestPermissions(requireActivity(), permissions, PERMISSION_CODE)
        }
    }

    // ?????? ?????? ????????? ??? ????????? ????????? ?????? ????????? ?????? ????????? argument??? ?????? ??? ??????
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == PERMISSION_CODE){
            if(grantResults.isNotEmpty()){
                for(grant in grantResults){
                    if(grant == PackageManager.PERMISSION_GRANTED) {
                        /*no-op*/
                    }else{
                        permissionDenied()
                        requirePermissions()
                    }
                }
            }
        }
    }
    // ????????? ?????? ?????? ??????
    private fun permissionDenied() {
        showToast("?????? ????????? ???????????????")
    }
}