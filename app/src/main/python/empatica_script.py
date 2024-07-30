import time
import asyncio
from pyempatica import EmpaticaClient, EmpaticaE4, EmpaticaDataStreams, EmpaticaServerConnectError
from bleak import BleakClient, BleakScanner

# Asynchronous function to connect to a Bluetooth device given its address
async def connect_bluetooth(address):
    try:
        # Use BleakScanner to find the Bluetooth device by its address
        device = await BleakScanner.find_device_by_address(address)
        if not device:
            print(f"Device with address {address} not found.")
            return None

        # Create a BleakClient instance for the found device
        client = BleakClient(device)
        # Connect to the Bluetooth device
        await client.connect()
        print(f"Connected to Bluetooth device at {address}")
        return client
    except Exception as err:
        # Handle any exceptions that occur during the connection process
        print(f"Failed to connect to Bluetooth device: {err}")
        return None

# Asynchronous function to collect data from the Empatica E4 device
async def collect_data(server_address):
    # Connect to the Bluetooth server
    client = await connect_bluetooth(server_address)
    if not client:
        return "Bluetooth connection failed"

    try:
        # Initialize the Empatica client
        empatica_client = EmpaticaClient()
        # List all connected Empatica devices
        empatica_client.list_connected_devices()

        if not empatica_client.device_list:
            print("No connected devices found.")
            return "No connected devices found"

        # Initialize the Empatica E4 device using the first device in the list
        e4 = EmpaticaE4(empatica_client.device_list[0])

        if not e4.connected:
            print("Could not connect to Empatica E4:", empatica_client.device_list[0])
            return f"Could not connect to Empatica E4: {empatica_client.device_list[0]}"

        # Subscribe to all available data streams from the Empatica E4
        for stream in EmpaticaDataStreams.ALL_STREAMS:
            e4.subscribe_to_stream(stream)

        # Start streaming data from the E4 device
        e4.start_streaming()

        # Collect data for 10 seconds, checking the device status every second
        for i in range(10):
            await asyncio.sleep(1)
            if not e4.on_wrist:
                print("E4 is not on wrist, please put it on!")
                return "E4 is not on wrist, please put it on!"
            if e4.client.last_error:
                print("Error encountered:", e4.client.last_error)
                return f"Error encountered: {e4.client.last_error}"

        # Suspend streaming and disconnect the E4 device
        e4.suspend_streaming()
        e4.disconnect()
        e4.close()
        # Save the collected data to a file
        e4.save_readings("readings.txt")

        return "Data collection successful"

    except EmpaticaServerConnectError:
        return "Failed to connect to server, check that the E4 Streaming Server is open and connected to the BLE dongle."
    except Exception as e:
        return f"An unexpected error occurred: {e}"
    finally:
        # Ensure the Bluetooth client is disconnected
        await client.disconnect()

# Synchronous function to process data collection
def process_data(server_address):
    try:
        # Run the asynchronous data collection function
        result = asyncio.run(collect_data(server_address))
        return result
    except Exception as e:
        return f"An error occurred: {e}"

if __name__ == "__main__":
    server_address = "2C:11:65:5A:6F:33"  # this is the address of the empatica e4
    print(process_data(server_address))
