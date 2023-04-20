package io.exonym.utils;

import java.io.File;

import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager;
import net.samuelcampos.usbdrivedetector.events.IUSBDriveListener;
import net.samuelcampos.usbdrivedetector.events.USBStorageEvent;

public abstract class UsbDeviceKeySwap implements IUSBDriveListener, AutoCloseable {
	
	private final USBDeviceDetectorManager device;
	
	public UsbDeviceKeySwap() {
		device = new USBDeviceDetectorManager();
		device.addDriveListener(this);
		device.setPollingInterval(1000);
		
	}

	@Override
	public void usbDriveEvent(USBStorageEvent arg0) {
		if (arg0.getStorageDevice().getRootDirectory()!=null){
			System.out.println("1." + arg0.getStorageDevice());
			
			if (arg0.getStorageDevice().canExecute()){
				System.out.println("2." + arg0.getStorageDevice());
				File f = arg0.getStorageDevice().getRootDirectory();
				foundNewRoot(f);
				
			}
		}
	}
	
	@Override
	public void close() throws Exception {
		device.removeDriveListener(this);
		
	}

	/**
	 * Create the PreAgreedKeys object and populate SymComKeys for the 
	 * client and server respectively.
	 * 
	 * @param root
	 */
	protected abstract void foundNewRoot(File root);

}
