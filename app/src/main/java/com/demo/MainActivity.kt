package com.demo

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.demo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Array<String>>

    private val foundDevices = mutableListOf<BluetoothDevice>()
    private val connectedDevices = mutableListOf<BluetoothDevice>()
    private val disconnectedDevices = mutableListOf<BluetoothDevice>()

    private val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            foundDevices.add(it)
                            // Check if the device is already connected
                            if (bluetoothAdapter.getBondedDevices().contains(it)) {
                                connectedDevices.add(it)
                            } else {
                                disconnectedDevices.add(it)
                            }
                            updateDeviceLists()
                            Toast.makeText(this@MainActivity, "Found device: ${it.name} - ${it.address}", Toast.LENGTH_SHORT).show()
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        Toast.makeText(this@MainActivity, "Bluetooth discovery finished", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Đăng ký ActivityResultLauncher để bật Bluetooth
        enableBluetoothLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                if (result.resultCode == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth đã được bật", Toast.LENGTH_SHORT).show()
                    binding.btnBlutooth.text = getString(R.string.disconnect)
                    startDiscovery()
                } else {
                    Toast.makeText(this, "Bluetooth chưa được bật", Toast.LENGTH_SHORT).show()
                    binding.btnBlutooth.text = getString(R.string.connect)
                }
            }

        // Đăng ký ActivityResultLauncher để xin quyền
        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { permissions ->
                if (permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                    permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                    permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                ) {
                    checkAndEnableBluetooth()
                } else {
                    Toast.makeText(this, "Quyền truy cập Bluetooth bị từ chối", Toast.LENGTH_SHORT).show()
                }
            }

        if (bluetoothAdapter == null) {
            // Thiết bị không hỗ trợ Bluetooth
            Toast.makeText(this, "Thiết bị không hỗ trợ Bluetooth", Toast.LENGTH_SHORT).show()
        } else {
            updateButtonText()
        }

        // Thiết lập RecyclerView cho thiết bị kết nối và chưa kết nối
        setupRecyclerViews()

        binding.btnBlutooth.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestBluetoothPermissions()
                } else {
                    toggleBluetooth()
                }
            } else {
                toggleBluetooth()
            }
        }

        // Đăng ký BroadcastReceiver để nhận các thiết bị được tìm thấy
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        registerReceiver(receiver, filter)
    }

    private fun setupRecyclerViews() {
        val connectedDevicesRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewConnected)
        val disconnectedDevicesRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewDisconnected)

        connectedDevicesRecyclerView.layoutManager = LinearLayoutManager(this)
        disconnectedDevicesRecyclerView.layoutManager = LinearLayoutManager(this)

        val connectedAdapter = DeviceAdapter(connectedDevices, DeviceType.CONNECTED)
        val disconnectedAdapter = DeviceAdapter(disconnectedDevices, DeviceType.DISCONNECTED)

        connectedDevicesRecyclerView.adapter = connectedAdapter
        disconnectedDevicesRecyclerView.adapter = disconnectedAdapter
    }

    private fun updateDeviceLists() {
        // Update RecyclerViews
        val connectedRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewConnected)
        val disconnectedRecyclerView = findViewById<RecyclerView>(R.id.recyclerViewDisconnected)

        (connectedRecyclerView.adapter as DeviceAdapter).notifyDataSetChanged()
        (disconnectedRecyclerView.adapter as DeviceAdapter).notifyDataSetChanged()
    }

    private fun updateButtonText() {
        if (bluetoothAdapter.isEnabled) {
            binding.btnBlutooth.text = getString(R.string.disconnect)
        } else {
            binding.btnBlutooth.text = getString(R.string.connect)
        }
    }

    private fun toggleBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
            Toast.makeText(this, "Bluetooth đã tắt", Toast.LENGTH_SHORT).show()
            updateButtonText()
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
            )
        }
    }

    private fun checkAndEnableBluetooth() {
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            Toast.makeText(this, "Bluetooth đang bật", Toast.LENGTH_SHORT).show()
            startDiscovery()
        }
    }

    private fun startDiscovery() {
        if (bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
        }
        bluetoothAdapter.startDiscovery()
        Toast.makeText(this, "Starting Bluetooth discovery", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký BroadcastReceiver khi không cần thiết nữa
        unregisterReceiver(receiver)
    }
}
