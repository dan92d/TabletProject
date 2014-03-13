package com.example.batterytest;
import android.content.Context;
import android.content.Intent;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.util.Log;

// Dovrò istanziare una classe poichè deve poter immagazzinare valori nel tempo.
public class BatteryListener extends Activity {
	// Impongo un massimo di 128 campioni per la stima.
	private final int MAX_SAMPLES_PER_ESTEEMATION = 128;
	
	// Queste variabili verranno utilizzate per il calcolo e la gesione dell'interfaccia.
	private int iBatteryRawLevel = -1;
	private int iBatteryRawScale = -1;
	private int iBatteryLevel = -1;
	private boolean bBatteryListenerHasToStop = false;
	private boolean bIsInterfaceRunning = false;
	private boolean bIsBatteryInCharge = false;
	
	private int iPreviousBatteryLevel = -1;
	private int iActualBatteryLevel = -1;
	private long lPreviousTimestamp = System.nanoTime();
	private long lActualTimestamp = System.nanoTime();
	
	private int iConsumption = -1;
	private long lConsumptionTime = -1;
	
	// Il primo è l'indice del vettore circolare delle stime.
	private int iBatteryResidualEsteemIndex = 0;
	// Un vettore di interi che sono i livelli della batteria.
	private int iBatteryResidualEsteemLevels[];
	// Un vettore di millisecondi: tempo di sistema alla variazione della batteria.
	private long iBatteryResidualEsteemMilliseconds[];
	
	/* Avvio l'interfaccia per la batteria, questo implica registrare un ricevitore
	 * allocare due vettori, uno di interi, l'altro di interi long.
	*/
    public void StartBatteryListener() {
    	
    	BroadcastReceiver brBatteryReceiver = new BroadcastReceiver() {
	    	public void onReceive(Context context, Intent intent) {
	    		if (bBatteryListenerHasToStop) {
	    			Log.i("BatteryListener","Broadcast receiver up to be unregistered.");
	    			context.unregisterReceiver(this);
	    		} else {
	    			Log.i("BatteryListener","Broadcast receiver registered.");
	    		}
	    		
	    		// Recupero il flag di caricamento.
	    		int iBatteryExtraStatus = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
	    		bIsBatteryInCharge = (iBatteryExtraStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
	    								iBatteryExtraStatus == BatteryManager.BATTERY_STATUS_FULL);
	    		
	    		iBatteryRawLevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
	    		iBatteryRawScale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
	    		iBatteryLevel = -1;
	    		Log.i("BatteryListener","RAW LEVEL: " + iBatteryRawLevel + "RAW SCALE: " + iBatteryRawScale);
	    		if (iBatteryRawLevel>=0 && iBatteryRawScale >0) {
	    			iBatteryLevel = iBatteryRawLevel / iBatteryRawScale * 100;
	    			
	    			if (iBatteryResidualEsteemMilliseconds!=null) {
	    				iBatteryResidualEsteemMilliseconds[iBatteryResidualEsteemIndex]=System.nanoTime();
	    			}
	    			if (iBatteryResidualEsteemLevels!=null) {
	    				iBatteryResidualEsteemLevels[iBatteryResidualEsteemIndex]=iBatteryLevel;
	    			}
	    			if (iBatteryResidualEsteemIndex<MAX_SAMPLES_PER_ESTEEMATION) {
	    				iBatteryResidualEsteemIndex++;
	    			} else {
	    				iBatteryResidualEsteemIndex=0;
	    			}
	    		}
	    	}
    	};
    	
    	// Se l'interfaccia non è ancora abilitata.
    	if (bIsInterfaceRunning == false) {
	    	Log.i("BatteryListener","Allocazione vettori imminente.");
	    	// Alloco i vettori di interi a 32 e 64 bits.
	    	iBatteryResidualEsteemLevels = new int[MAX_SAMPLES_PER_ESTEEMATION];
	    	iBatteryResidualEsteemMilliseconds = new long[MAX_SAMPLES_PER_ESTEEMATION];
	    	// Riempio il vettore con il valore attuale +/- pochi ms.
	    	for (int i=0; i<MAX_SAMPLES_PER_ESTEEMATION; i++) {
	    		iBatteryResidualEsteemMilliseconds[i]=System.nanoTime();
	    	}
	    	Log.i("BatteryListener","Allocazione avvenuta.");
	    	// Imposto l'attività dell'interfaccia a true. (ciù)
	    	bIsInterfaceRunning = true;
    		
	    	// Apparentemente dopo l'istanziazione dell'intent nessun codice è eseguito a seguire.
    		Log.i("BatteryListener","Istanziazione dell'intent filter imminente.");
    		// Registro il ricevitore dell'evento.
	    	IntentFilter ifBatteryFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
	    	registerReceiver(brBatteryReceiver, ifBatteryFilter);
	    	Log.i("BatteryListener","Istanziazione dell'intent completata.");
    	}
    }
    
    private void ComputeBatteryStatistics(Context context, Intent intent) {
    	
    }
    /*
     * Blocca il ricevitore della batteria. Imposta solo le variabili per la disabilitazione.
     * La cancellazione della sottoscrizione al listener avverrà al primo cambiamento della batteria.
     * TODO: Cancellare la sottoscrizione immediatamente, usare la classe: Context.
    */
    public void StopBatteryListener() {
    	this.bBatteryListenerHasToStop = true;
    	this.bIsInterfaceRunning = false;
    	this.iBatteryResidualEsteemLevels = null;
    	this.iBatteryResidualEsteemMilliseconds = null;
    }
    
    // Ottengo la variabile Level.
    public int GetBatteryLife() {
    	return this.iBatteryLevel;
    }
    // Calcolo la stima della batteria residua.
    public int GetBatteryResidualTime() {
    	int iAverageBatteryDischargePeriod = 0;
    	int iEsteemedResidualTime = 0;
    	// Calcolo la media tra tutti gli intervalli di tempo.
    	for (int i=0; i<MAX_SAMPLES_PER_ESTEEMATION; i++) {
    		iAverageBatteryDischargePeriod+=iBatteryResidualEsteemMilliseconds[i];
    	}
    	iAverageBatteryDischargePeriod/=MAX_SAMPLES_PER_ESTEEMATION;
    	// Ritorno il prodotto della media per il livello di batteria attuale.
    	return iEsteemedResidualTime = iAverageBatteryDischargePeriod * iBatteryLevel;
    }
    
    public long GetBatteryComsumption()
    {
    	iPreviousBatteryLevel = iActualBatteryLevel;
    	iActualBatteryLevel = this.iBatteryLevel;
    	
    	lPreviousTimestamp = lActualTimestamp;
    	lActualTimestamp = System.nanoTime();
    	
    	if ( this.bIsBatteryInCharge && (iPreviousBatteryLevel < iActualBatteryLevel) )
    		{
    			iPreviousBatteryLevel = 0;
    			iActualBatteryLevel = 0;
    			return 0;
    		}
    		else
    		{
    			iConsumption+=(iPreviousBatteryLevel - iActualBatteryLevel);
    			lConsumptionTime+=(lActualTimestamp - lPreviousTimestamp);
    			return iConsumption / lConsumptionTime;
    			// consumo della batteria la cui unità di misura del tempo è quella ritornata da System.nanoTime();()
    			// es. 6,3 % per ogni secondo o millisecondo oppure ora
    		}
    }
}
/*
 * Come usare?
 * 
 * BatteryListener biBatteryListener = new BatteryListener();
 * biBatteryListener.StartBatteryListener();
 * 
 * ...
 * Test
 * biBatteryListener.GetBatteryLife();
 * biBatteryListener.GetBatteryResidualTime();
 * ...
 * 
 * biBatteryListener.StopBatteryListener();
 * biBatteryListener = null;
 * 
*/