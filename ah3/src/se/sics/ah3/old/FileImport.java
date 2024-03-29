package se.sics.ah3.old;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


import android.R.bool;
import android.os.Environment;
import android.util.Log;

public class FileImport {
	
	public enum Modes { RAW, AH2HISTORY, AH2HISTORYBATCH, AH2HISTORY_T, AH2HISTORYBATCH_T  }
	
	private BufferedReader ah2in;
	private String line;
	private String[] tokens;
	private History history = null;
	private DataStore dataStore = null;
	GSRProcessor _gsrProcessor = null;
	AccelProcessor _accProcessor = null;
	EcgProcessor _ecgProcessor = null;
	int _errorRate = 0;
	BioPlux _bioPlux = null;
	int lastArousal = -1;
	int lastMovement = -1;
	int lastPulse = -1;

	public FileImport(History aHistory, DataStore ds) {
		history = aHistory;
		dataStore = ds;
	}
	
	private void initRAWmode()
	{
		_gsrProcessor = new GSRProcessor();
		_accProcessor = new AccelProcessor();
		_ecgProcessor = new EcgProcessor();
		_bioPlux = new BioPlux();
	}
	
	public void importFile(String fileName, Modes mode)
	{
		switch(mode)
		{
		case RAW:
			initRAWmode();
			readFromRawRecording(fileName);
			break;
		case AH2HISTORY_T:
			readFromAH2HistoryFile(fileName);
			break;
		case AH2HISTORYBATCH_T:
			readDayFromAH2HistoryFiles(fileName);
			break;
		default:
			break;
		}
	}
	private void readDayFromAH2HistoryFiles(String fileNamePattern)
	{
		//EAHHeartRate = 64,
		//EAHArousal = 128,
		//EAHArousalS = 136,
		//EAHMovement = 192,
		String fileName = fileNamePattern+"64_1T";
		readFromAH2HistoryFile(fileName);
		fileName = fileNamePattern+"128_1T";
		readFromAH2HistoryFile(fileName);
		fileName = fileNamePattern+"192_1T";
		readFromAH2HistoryFile(fileName);
	}
	
	private void readFromRawAh2HistoryFile(String fileName)
	{
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canRead()){
				File rawFile = new File(root, fileName);
				FileReader rawReader = new FileReader(rawFile);
				ah2in = new BufferedReader(rawReader);
				line = ah2in.readLine();
				int importedLines = 0;
				int secondsProcessed = 0;
				String dataString;
				long timeStamp = 0;
				History.Channels c = History.determinChannel(fileName);
				short val = -121;
				DateFormat formatter;
				Date date;
				while(line.length() > 0)
				{
					line = line.replaceAll("\\p{Cntrl}", "");
					importedLines++;
					if(importedLines%1000 == 0)
					{
						Log.i("AH2FileImporter", "imported "+ importedLines+" lines of file "+ fileName);
					}
					if(line.startsWith("Val:"))
					{
						dataString = line.substring(4);
						while(dataString.startsWith("0"))
						{
							dataString = dataString.substring(1);
						}
						val = Short.parseShort(dataString);
						if(val >= 0)
							History.updateToDbByTime(dataStore, timeStamp, val, c);
						timeStamp += 1000;
					}
					else
					{
						if(line.startsWith("FileStart: "))
						{
							dataString = line.substring(11);
							try {
							    // 2 0 1 0 . 1 1 . 3 0   -   0 8 : 0 2 : 3 1 
							    formatter = new SimpleDateFormat("yyyy.MM.dd - hh:mm:ss");
							    date = formatter.parse(dataString);
								timeStamp = date.getTime();
							} catch (ParseException e) {
							}
						}
					}
					if(importedLines >= 999)
					{
						importedLines = 0;
						secondsProcessed++;
						history.setBufferdCreateValues((short)lastArousal,(short) lastMovement, (short)lastPulse, dataStore);
						//history.saveToDb(dataStore);
						//if(secondsProcessed >= 100)
						//	return;
					}
					line = ah2in.readLine();
				}
			}
		} catch (IOException e) {
			Log.e("Raw line reading", "Could not read file " + e.getMessage());
			ah2in = null;
			return;
		}
	}
	private void readFromAH2HistoryFile(String fileName)
	{
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canRead()){
				File rawFile = new File(root, fileName);
				FileReader rawReader = new FileReader(rawFile);
				ah2in = new BufferedReader(rawReader);
				line = ah2in.readLine();
				int importedLines = 0;
				int secondsProcessed = 0;
				String dataString;
				long timeStamp = 0;
				History.Channels c = History.determinChannel(fileName);
				short val = -121;
				DateFormat formatter;
				Date date;
				while(line.length() > 0)
				{
					line = line.replaceAll("\\p{Cntrl}", "");
					importedLines++;
					if(importedLines%1000 == 0)
					{
						Log.i("AH2FileImporter", "imported "+ importedLines+" lines of file "+ fileName);
					}
					if(line.startsWith("Val:"))
					{
						dataString = line.substring(4);
						while(dataString.startsWith("0"))
						{
							dataString = dataString.substring(1);
						}
						val = Short.parseShort(dataString);
						if(val >= 0)
							History.updateToDbByTime(dataStore, timeStamp, val, c);
						timeStamp += 1000;
					}
					else
					{
						if(line.startsWith("FileStart: "))
						{
							dataString = line.substring(11);
							try {
							    // 2 0 1 0 . 1 1 . 3 0   -   0 8 : 0 2 : 3 1 
							    formatter = new SimpleDateFormat("yyyy.MM.dd - hh:mm:ss");
							    date = formatter.parse(dataString);
								timeStamp = date.getTime();
							} catch (ParseException e) {
							}
						}
					}
					if(importedLines >= 999)
					{
						importedLines = 0;
						secondsProcessed++;
						history.setBufferdCreateValues((short)lastArousal,(short) lastMovement, (short)lastPulse, dataStore);
						//history.saveToDb(dataStore);
						//if(secondsProcessed >= 100)
						//	return;
					}
					line = ah2in.readLine();
				}
			}
		} catch (IOException e) {
			Log.e("Raw line reading", "Could not read file " + e.getMessage());
			ah2in = null;
			return;
		}
	}

	private void readFromRawRecording(String fileName)//"dance.txt"
	{
		try {
			File root = Environment.getExternalStorageDirectory();
			if (root.canRead()){
				File rawFile = new File(root, fileName);
				FileReader rawReader = new FileReader(rawFile);
				ah2in = new BufferedReader(rawReader);
				line = ah2in.readLine();
				int importedLines = 0;
				int secondsProcessed = 0;
				while(line.length() > 0)
				{
					importedLines++;
					tokens = line.split("\t");
					if(tokens.length == 10)
					{
						_bioPlux.setValues(tokens);
						lastArousal = _bioPlux.processArousal(_gsrProcessor);
						lastMovement = _bioPlux.processMovement(_accProcessor);
						lastPulse = 0;//_bioPlux.processPulse(_ecgProcessor);
					}
					if(importedLines >= 999)
					{
						importedLines = 0;
						secondsProcessed++;
						history.setBufferdCreateValues((short)lastArousal,(short) lastMovement, (short)lastPulse, dataStore);
						//history.saveToDb(dataStore);
						//if(secondsProcessed >= 100)
						//	return;
					}
					
					for(int i = 0; i < tokens.length; i++)
					{
						//tokens[i];
						//Log.d("Raw line reading:", tokens[i]);
						
					}
					line = ah2in.readLine();
				}
			}
		} catch (IOException e) {
			Log.e("Raw line reading", "Could not read file " + e.getMessage());
			ah2in = null;
			return;
		}
	}
}
