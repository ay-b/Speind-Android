package com.maple.speind;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import me.speind.SpeindAPI;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;

import com.acapelagroup.android.tts.acattsandroid;
import com.acapelagroup.android.tts.acattsandroid.iTTSEventsCallback;

public class AcapelaReader extends TextReader implements iTTSEventsCallback {
	private static final boolean isDebug = false;

	private static final String PREFS_NAME = "voicesConfig";
	private String VOCES_DIR = SpeindAPI.SPEIND_DIR + "acapelavoices" + File.separator;
	private boolean isRead=false;
	private boolean needSendCompleteRead=false;
	private String readText="";
	private String readLang="";
    private acattsandroid TTS=null; 
    private Context mContext=null;

	private float speech_rate = 100.0f;

    private long lastPos;
    private long readedBefore;
    
	private final Handler handler = new Handler();
	private final Runnable autocompleteRunnable = new Runnable() { public void run() { sendCompleteRead(); } };
    
    private static final Map<String, String> voiceFiles = new HashMap<String, String>();
    static {
    	voiceFiles.put("Bente",				"hq-ref-Norwegian-Bente-22khz.zip");
    	voiceFiles.put("Kari",				"hq-ref-Norwegian-Kari-22khz.zip");
    	voiceFiles.put("Olav",				"hq-ref-Norwegian-Olav-22khz.zip");
    	voiceFiles.put("Marcia",			"hq-ref-Brazilian-Marcia-22khz.zip");
    	voiceFiles.put("Laia",				"hq-ref-Catalan-Laia-22khz.zip");
    	voiceFiles.put("Eliska",			"hq-ref-Czech-Eliska-22khz.zip");
    	voiceFiles.put("Mette",				"hq-ref-Danish-Mette-22khz.zip");
    	voiceFiles.put("Rasmus",			"hq-ref-Danish-Rasmus-22khz.zip");
    	voiceFiles.put("Andreas",			"hq-ref-German-Andreas-22khz.zip");
    	voiceFiles.put("Julia",				"hq-ref-German-Julia-22khz.zip");
    	voiceFiles.put("Klaus",				"hq-ref-German-Klaus-22khz.zip");
    	voiceFiles.put("Sarah",				"hq-ref-German-Sarah-22khz.zip");
    	voiceFiles.put("Lisa",				"hq-ref-AustralianEnglish-Lisa-22khz.zip");
    	voiceFiles.put("Tyler",				"hq-ref-AustralianEnglish-Tyler-22khz.zip");
    	voiceFiles.put("Graham",			"hq-ref-British-Graham-22khz.zip");
    	voiceFiles.put("Lucy",				"hq-ref-British-Lucy-22khz.zip");
    	voiceFiles.put("Nizareng",			"hq-ref-British-Nizareng-22khz.zip");
    	voiceFiles.put("Peter",				"hq-ref-British-Peter-22khz.zip");
    	voiceFiles.put("Peterhappy",		"hq-ref-British-Peterhappy-22khz.zip");
    	voiceFiles.put("Petersad",			"hq-ref-British-Petersad-22khz.zip");
    	voiceFiles.put("Queenelizabeth",	"hq-ref-British-Queenelizabeth-22khz.zip");
    	voiceFiles.put("Rachel",			"hq-ref-British-Rachel-22khz.zip");
    	voiceFiles.put("Deepa",				"hq-ref-IndianEnglish-Deepa-22khz.zip");
    	voiceFiles.put("Heather",			"hq-ref-USEnglish-Heather-22khz.zip");
    	voiceFiles.put("Karen",				"hq-ref-USEnglish-Karen-22khz.zip");
    	voiceFiles.put("Kenny",				"hq-ref-USEnglish-Kenny-22khz.zip");
    	voiceFiles.put("Laura",				"hq-ref-USEnglish-Laura-22khz.zip");
    	voiceFiles.put("Micah",				"hq-ref-USEnglish-Micah-22khz.zip");
    	voiceFiles.put("Nelly",				"hq-ref-USEnglish-Nelly-22khz.zip");
    	voiceFiles.put("Ryan",				"hq-ref-USEnglish-Ryan-22khz.zip");
    	voiceFiles.put("Saul",				"hq-ref-USEnglish-Saul-22khz.zip");
    	voiceFiles.put("Tracy",				"hq-ref-USEnglish-Tracy-22khz.zip");
    	voiceFiles.put("Will",				"hq-ref-USEnglish-Will-22khz.zip");
    	voiceFiles.put("Willbadguy",		"hq-ref-USEnglish-Willbadguy-22khz.zip");
    	voiceFiles.put("Willfromafar",		"hq-ref-USEnglish-Willfromafar-22khz.zip");
    	voiceFiles.put("Willhappy",			"hq-ref-USEnglish-Willhappy-22khz.zip");
    	voiceFiles.put("Willlittlecreature","hq-ref-USEnglish-Willlittlecreature-22khz.zip");
    	voiceFiles.put("Willoldman",		"hq-ref-USEnglish-Willoldman-22khz.zip");
    	voiceFiles.put("Willsad",			"hq-ref-USEnglish-Willsad-22khz.zip");
    	voiceFiles.put("Willupclose",		"hq-ref-USEnglish-Willupclose-22khz.zip");
    	voiceFiles.put("Antonio",			"hq-ref-Spanish-Antonio-22khz.zip");
    	voiceFiles.put("Ines",				"hq-ref-Spanish-Ines-22khz.zip");
    	voiceFiles.put("Maria",				"hq-ref-Spanish-Maria-22khz.zip");
    	voiceFiles.put("Rosa",				"hq-ref-USSpanish-Rosa-22khz.zip");
    	voiceFiles.put("Alice",				"hq-ref-French-Alice-22khz.zip");
    	voiceFiles.put("Antoine",			"hq-ref-French-Antoine-22khz.zip");
    	voiceFiles.put("Antoinefromafar",	"hq-ref-French-Antoinefromafar-22khz.zip");
    	voiceFiles.put("Antoinehappy",		"hq-ref-French-Antoinehappy-22khz.zip");
    	voiceFiles.put("Antoinesad",		"hq-ref-French-Antoinesad-22khz.zip");
    	voiceFiles.put("Antoineupclose",	"hq-ref-French-Antoineupclose-22khz.zip");
    	voiceFiles.put("Bruno",				"hq-ref-French-Bruno-22khz.zip");
    	voiceFiles.put("Claire",			"hq-ref-French-Claire-22khz.zip");
    	voiceFiles.put("Julie",				"hq-ref-French-Julie-22khz.zip");
    	voiceFiles.put("Margaux",			"hq-ref-French-Margaux-22khz.zip");
    	voiceFiles.put("Margauxhappy",		"hq-ref-French-Margauxhappy-22khz.zip");
    	voiceFiles.put("Margauxsad",		"hq-ref-French-Margauxsad-22khz.zip");
    	voiceFiles.put("Robot",				"hq-ref-French-Robot-22khz.zip");
    	voiceFiles.put("Louise",			"hq-ref-CanadianFrench-Louise-22khz.zip");
    	voiceFiles.put("Chiara",			"hq-ref-Italian-Chiara-22khz.zip");
    	voiceFiles.put("Fabiana",			"hq-ref-Italian-Fabiana-22khz.zip");
    	voiceFiles.put("Vittorio",			"hq-ref-Italian-Vittorio-22khz.zip");
    	voiceFiles.put("Jeroen",			"hq-ref-BelgianDutch-Jeroen-22khz.zip");
    	voiceFiles.put("Jeroenhappy",		"hq-ref-BelgianDutch-Jeroenhappy-22khz.zip");
    	voiceFiles.put("Jeroensad",			"hq-ref-BelgianDutch-Jeroensad-22khz.zip");
    	voiceFiles.put("Sofie",				"hq-ref-BelgianDutch-Sofie-22khz.zip");
    	voiceFiles.put("Zoe",				"hq-ref-BelgianDutch-Zoe-22khz.zip");
    	voiceFiles.put("Daan",				"hq-ref-Dutch-Daan-22khz.zip");
    	voiceFiles.put("Femke",				"hq-ref-Dutch-Femke-22khz.zip");
    	voiceFiles.put("Jasmijn",			"hq-ref-Dutch-Jasmijn-22khz.zip");
    	voiceFiles.put("Max",				"hq-ref-Dutch-Max-22khz.zip");
    	voiceFiles.put("Ania",				"hq-ref-Polish-Ania-22khz.zip");
    	voiceFiles.put("Monika",			"hq-ref-Polish-Monika-22khz.zip");
    	voiceFiles.put("Celia",				"hq-ref-Portuguese-Celia-22khz.zip");
    	voiceFiles.put("Mia",				"hq-ref-Scanian-Mia-22khz.zip");
    	voiceFiles.put("Sanna",				"hq-ref-Finnish-Sanna-22khz.zip");
    	voiceFiles.put("Samuel",			"hq-ref-FinlandSwedish-Samuel-22khz.zip");
    	voiceFiles.put("Kal",				"hq-ref-GothenburgSwedish-Kal-22khz.zip");
    	voiceFiles.put("Elin",				"hq-ref-Swedish-Elin-22khz.zip");
    	voiceFiles.put("Emil",				"hq-ref-Swedish-Emil-22khz.zip");
    	voiceFiles.put("Emma",				"hq-ref-Swedish-Emma-22khz.zip");
    	voiceFiles.put("Erik",				"hq-ref-Swedish-Erik-22khz.zip");
    	voiceFiles.put("Ipek",				"hq-ref-Turkish-Ipek-22khz.zip");
    	voiceFiles.put("Dimitris",			"hq-ref-Greek-Dimitris-22khz.zip");
    	voiceFiles.put("DimitrisHappy",		"hq-ref-Greek-DimitrisHappy-22khz.zip");
    	voiceFiles.put("DimitrisSad",		"hq-ref-Greek-DimitrisSad-22khz.zip");
    	voiceFiles.put("Alyona",			"hq-ref-Russian-Alyona-22khz.zip");
    	voiceFiles.put("Leila",				"hq-ref-Arabic-Leila-22khz.zip");
    	voiceFiles.put("Mehdi",				"hq-ref-Arabic-Mehdi-22khz.zip");
    	voiceFiles.put("Nizar",				"hq-ref-Arabic-Nizar-22khz.zip");
    	voiceFiles.put("Salma",				"hq-ref-Arabic-Salma-22khz.zip");
    	voiceFiles.put("Minji",				"hq-ref-Korean-Minji-22khz.zip");
    	voiceFiles.put("Lulu",				"hq-ref-MandarinChinese-Lulu-22khz.zip");
    	voiceFiles.put("Sakura",			"hq-ref-Japanese-Sakura-22khz.zip");
    }

    private static final Map<String, String> voicesSKUs = new HashMap<String, String>();
    static {
    	voicesSKUs.put("Bente",				"");
    	voicesSKUs.put("Kari",				"");
    	voicesSKUs.put("Olav",				"");
    	voicesSKUs.put("Marcia",			"");
    	voicesSKUs.put("Laia",				"");
    	voicesSKUs.put("Eliska",			"");
    	voicesSKUs.put("Mette",				"");
    	voicesSKUs.put("Rasmus",			"");
    	voicesSKUs.put("Andreas",			"");
    	voicesSKUs.put("Julia",				"");
    	voicesSKUs.put("Klaus",				"");
    	voicesSKUs.put("Sarah",				"");
    	voicesSKUs.put("Lisa",				"");
    	voicesSKUs.put("Tyler",				"");
    	voicesSKUs.put("Graham",			"graham_english");
    	voicesSKUs.put("Lucy",				"lucy_english");
    	voicesSKUs.put("Nizareng",			"nizar_english");
    	voicesSKUs.put("Peter",				"peter_english");
    	voicesSKUs.put("Peterhappy",		"peterhappy_english");
    	voicesSKUs.put("Petersad",			"petersad_english");
    	voicesSKUs.put("Queenelizabeth",	"queenelizabeth_english");
    	voicesSKUs.put("Rachel",			"rachel_english");
    	voicesSKUs.put("Deepa",				"");
    	voicesSKUs.put("Heather",			"heather_english");
    	voicesSKUs.put("Karen",				"karen_english");
    	voicesSKUs.put("Kenny",				"kenny_english");
    	voicesSKUs.put("Laura",				"laura_english");
    	voicesSKUs.put("Micah",				"mikah_english");
    	voicesSKUs.put("Nelly",				"nelly_english");
    	voicesSKUs.put("Ryan",				"ryan_english");
    	voicesSKUs.put("Saul",				"saul_english");
    	voicesSKUs.put("Tracy",				"tracy_english");
    	voicesSKUs.put("Will",				"will_english");
    	voicesSKUs.put("Willbadguy",		"willbadguy_english");
    	voicesSKUs.put("Willfromafar",		"willfromafar_english");
    	voicesSKUs.put("Willhappy",			"willhappy_english");
    	voicesSKUs.put("Willlittlecreature","willlittlecreature_english");
    	voicesSKUs.put("Willoldman",		"willoldman_english");
    	voicesSKUs.put("Willsad",			"willsad_english");
    	voicesSKUs.put("Willupclose",		"willupclose_english");
    	voicesSKUs.put("Antonio",			"");
    	voicesSKUs.put("Ines",				"");
    	voicesSKUs.put("Maria",				"");
    	voicesSKUs.put("Rosa",				"");
    	voicesSKUs.put("Alice",				"alice_french");
    	voicesSKUs.put("Antoine",			"antoine_french");
    	voicesSKUs.put("Antoinefromafar",	"antoinefromafar_french");
    	voicesSKUs.put("Antoinehappy",		"antoinehappy_french");
    	voicesSKUs.put("Antoinesad",		"antoinesad_french");
    	voicesSKUs.put("Antoineupclose",	"antoineupclose_french");
    	voicesSKUs.put("Bruno",				"bruno_french");
    	voicesSKUs.put("Claire",			"claire_french");
    	voicesSKUs.put("Julie",				"julie_french");
    	voicesSKUs.put("Margaux",			"margaux_french");
    	voicesSKUs.put("Margauxhappy",		"margauxhappy_french");
    	voicesSKUs.put("Margauxsad",		"margauxsad_french");
    	voicesSKUs.put("Robot",				"robot_french");
    	voicesSKUs.put("Louise",			"");
    	voicesSKUs.put("Chiara",			"");
    	voicesSKUs.put("Fabiana",			"");
    	voicesSKUs.put("Vittorio",			"");
    	voicesSKUs.put("Jeroen",			"");
    	voicesSKUs.put("Jeroenhappy",		"");
    	voicesSKUs.put("Jeroensad",			"");
    	voicesSKUs.put("Sofie",				"");
    	voicesSKUs.put("Zoe",				"");
    	voicesSKUs.put("Daan",				"");
    	voicesSKUs.put("Femke",				"");
    	voicesSKUs.put("Jasmijn",			"");
    	voicesSKUs.put("Max",				"");
    	voicesSKUs.put("Ania",				"");
    	voicesSKUs.put("Monika",			"");
    	voicesSKUs.put("Celia",				"");
    	voicesSKUs.put("Mia",				"");
    	voicesSKUs.put("Sanna",				"");
    	voicesSKUs.put("Samuel",			"");
    	voicesSKUs.put("Kal",				"");
    	voicesSKUs.put("Elin",				"");
    	voicesSKUs.put("Emil",				"");
    	voicesSKUs.put("Emma",				"");
    	voicesSKUs.put("Erik",				"");
    	voicesSKUs.put("Ipek",				"");
    	voicesSKUs.put("Dimitris",			"");
    	voicesSKUs.put("DimitrisHappy",		"");
    	voicesSKUs.put("DimitrisSad",		"");
    	voicesSKUs.put("Alyona",			"alyona_russian");
    	voicesSKUs.put("Leila",				"");
    	voicesSKUs.put("Mehdi",				"");
    	voicesSKUs.put("Nizar",				"");
    	voicesSKUs.put("Salma",				"");
    	voicesSKUs.put("Minji",				"");
    	voicesSKUs.put("Lulu",				"");
    	voicesSKUs.put("Sakura",			"");
    }
    
    @SuppressWarnings("unchecked")
	private ArrayList<ArrayList<String>> voicesData=new ArrayList<ArrayList<String>>(Arrays.asList(
    		//new ArrayList<String>(Arrays.asList("bokmål",		"Bente",							"Bente",				"non",	"0", "non_bente_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("bokmål",		"Kari",								"Kari",					"non",	"0", "non_kari_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("bokmål",		"Olav",								"Olav",					"non",	"0", "non_olav_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Brazilian",	"Marcia",							"Marcia",				"br",	"0", "pob_marcia_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("català",		"Laia",								"Laia",					"ca",	"0", "ca_es_laia_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("čeština",	"Eliska",							"Eliska",				"sl",	"0", "czc_eliska_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Dansk",		"Mette",							"Mette",				"da",	"0", "dad_mette_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Dansk",		"Rasmus",							"Rasmus",				"da",	"0", "dad_rasmus_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Deutsch",	"Andreas",							"Andreas",				"de",	"0", "ged_andreas_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Deutsch",	"Julia",							"Julia",				"de",	"0", "ged_julia_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Deutsch",	"Klaus",							"Klaus",				"de",	"0", "ged_klaus_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Deutsch",	"Sarah",							"Sarah",				"de",	"0", "ged_sarah_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("English",	"Lisa [AustralianEnglish]",			"Lisa",					"en",	"0", "en_au_lisa_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("English",	"Tyler [AustralianEnglish]",		"Tyler",				"en",	"0", "en_au_tyler_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Graham [British]",					"Graham",				"en",	"0", "eng_graham_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Lucy [British]",					"Lucy",					"en",	"0", "eng_lucy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Nizar eng [British]",				"Nizareng",				"en",	"0", "eng_nizareng_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Peter [British]",					"Peter",				"en",	"0", "eng_peter_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Peter Happy [British]",			"Peterhappy",			"en",	"0", "eng_peterhappy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Peter Sad [British]",				"Petersad",				"en",	"0", "eng_petersad_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Queen Elizabeth [British]",		"Queenelizabeth",		"en",	"0", "eng_queenelizabeth_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Rachel [British]",					"Rachel",				"en",	"0", "eng_rachel_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("English",	"Deepa [IndianEnglish]",			"Deepa",				"en",	"0", "en_in_deepa_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Heather [USEnglish]",				"Heather",				"en",	"0", "enu_heather_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Karen [USEnglish]",				"Karen",				"en",	"0", "enu_karen_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Kenny [USEnglish]",				"Kenny",				"en",	"0", "enu_kenny_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Laura [USEnglish]",				"Laura",				"en",	"0", "enu_laura_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Micah [USEnglish]",				"Micah",				"en",	"0", "enu_micah_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Nelly [USEnglish]",				"Nelly",				"en",	"0", "enu_nelly_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Ryan [USEnglish]",					"Ryan",					"en",	"0", "enu_ryan_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Saul [USEnglish]",					"Saul",					"en",	"0", "enu_saul_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Tracy [USEnglish]",				"Tracy",				"en",	"0", "enu_tracy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will [USEnglish]",					"Will",					"en",	"0", "enu_will_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Bad-guy [USEnglish]",			"Willbadguy",			"en",	"0", "enu_willbadguy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Aloud [USEnglish]",			"Willfromafar",			"en",	"0", "enu_willfromafar_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Happy [USEnglish]",			"Willhappy",			"en",	"0", "enu_willhappy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Little Creature [USEnglish]",	"Willlittlecreature",	"en",	"0", "enu_willlittlecreature_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Old Man [USEnglish]",			"Willoldman",			"en",	"0", "enu_willoldman_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Sad [USEnglish]",				"Willsad",				"en",	"0", "enu_willsad_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("English",		"Will Close [USEnglish]",			"Willupclose",			"en",	"0", "enu_willupclose_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Español",	"Antonio",							"Antonio",				"es",	"0", "sps_antonio_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Español",	"Ines",								"Ines",					"es",	"0", "sps_ines_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Español",	"Maria",							"Maria",				"es",	"0", "sps_maria_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Español",	"Rosa [USSpanish]",					"Rosa",					"es",	"0", "spu_rosa_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Alice",							"Alice",				"fr",	"0", "frf_alice_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Antonie",							"Antoine",				"fr",	"0", "frf_antoine_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Antoine Aloud",					"Antoinefromafar",		"fr",	"0", "frf_antoinefromafar_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Antoine Happy",					"Antoinehappy",			"fr",	"0", "frf_antoinehappy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Antoine Sad",						"Antoinesad",			"fr",	"0", "frf_antoinesad_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Antoine Close",					"Antoineupclose",		"fr",	"0", "frf_antoineupclose_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Bruno",							"Bruno",				"fr",	"0", "frf_bruno_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Claire",							"Claire",				"fr",	"0", "frf_claire_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Julie",							"Julie",				"fr",	"0", "frf_julie_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Margaux",							"Margaux",				"fr",	"0", "frf_margaux_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Margaux Happy",					"Margauxhappy",			"fr",	"0", "frf_margauxhappy_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Margaux Sad",						"Margauxsad",			"fr",	"0", "frf_margauxsad_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("français",	"Robot",							"Robot",				"fr",	"0", "frf_robot_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Français",	"Louise [CanadianFrench]",			"Louise",				"fr",	"0", "frc_louise_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("italiano",	"Chiara",							"Chiara",				"it",	"0", "iti_chiara_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("italiano",	"Fabiana",							"Fabiana",				"it",	"0", "iti_fabiana_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("italiano",	"Vittorio",							"Vittorio",				"it",	"0", "iti_vittorio_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Jeroen [BelgianDutch]",			"Jeroen",				"nl",	"0", "dub_jeroen_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Jeroen Happy [BelgianDutch]",		"Jeroenhappy",			"nl",	"0", "dub_jeroenhappy_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Jeroen Sad [BelgianDutch]",		"Jeroensad",			"nl",	"0", "dub_jeroensad_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Sofie [BelgianDutch]",				"Sofie",				"nl",	"0", "dub_sofie_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Zoe [BelgianDutch]",				"Zoe",					"nl",	"0", "dub_zoe_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Daan",								"Daan",					"nl",	"0", "dun_daan_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Femke",							"Femke",				"nl",	"0", "dun_femke_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Jasmin",							"Jasmijn",				"nl",	"0", "dun_jasmijn_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Nederlands",	"Max",								"Max",					"nl",	"0", "dun_max_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("polski",		"Ania",								"Ania",					"pl",	"0", "pop_ania_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("polski",		"Monika",							"Monika",				"pl",	"0", "pop_monika_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Português",	"Celia",							"Celia",				"pt",	"0", "poe_celia_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Scanian",	"Mia",								"Mia",					"snl",	"0", "sc_se_mia_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("suomi",		"Sanna",							"Sanna",				"fi",	"0", "fif_sanna_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Samuel [FinlandSwedish]",			"Samuel",				"sv",	"0", "sv_fi_samuel_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Kal [GothenburgSwedish]",			"Kal",					"sv",	"0", "gb_se_kal_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Elin",								"Elin",					"sv",	"0", "sws_elin_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Emil",								"Emil",					"sv",	"0", "sws_emil_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Emma",								"Emma",					"sv",	"0", "sws_emma_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("svenska",	"Erik",								"Erik",					"sv",	"0", "sws_erik_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("Türkçe",		"Ipek",								"Ipek",					"tr",	"0", "tut_ipek_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ελληνικά",	"Dimitris",							"Dimitris",				"el",	"0", "grg_dimitris_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ελληνικά",	"Dimitris Happy",					"DimitrisHappy",		"el",	"0", "grg_dimitrishappy_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ελληνικά",	"Dimitris Sad",						"DimitrisSad",			"el",	"0", "grg_dimitrissad_22k_ns.qvcu.mp3")),
    		new ArrayList<String>(Arrays.asList("русский",		"Алёна",							"Alyona",				"ru",	"0", "rur_alyona_22k_ns.qvcu.mp3"))
    		//new ArrayList<String>(Arrays.asList("ﺔﻴﺐﺮﻌﻠﺍ",		"Leila",							"Leila",				"ar",	"0", "ar_sa_leila_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ﺔﻴﺐﺮﻌﻠﺍ",		"Mehdi",							"Mehdi",				"ar",	"0", "ar_sa_mehdi_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ﺔﻴﺐﺮﻌﻠﺍ",		"Nizar",							"Nizar",				"ar",	"0", "ar_sa_nizar_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("ﺔﻴﺐﺮﻌﻠﺍ",		"Salma",							"Salma",				"ar",	"0", "ar_sa_salma_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("한국어",		"Minji",							"Minji",				"ko",	"0", "ko_kr_minji_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("中文",		"Lulu [MandarinChinese]",			"Lulu",					"zh-cn",	"0", "zh_cn_lulu_22k_ns.qvcu.mp3")),
    		//new ArrayList<String>(Arrays.asList("日本語",		"Sakura",							"Sakura",				"ja",	"0", "ja_jp_sakura_22k_ns.qvcu.mp3"))
    ));
    
    
	public AcapelaReader(Context context, IReaderEventsCallback readerEventsCallback, int id) {
		super(context, readerEventsCallback, id);
		mContext=context;		
		TTS = new acattsandroid(this.mContext, this, null);
		TTS.setTTSSettings("AUDIOTRACKCOEFFICIENT", 5);
		TTS.setLicense(0x010ce8a7,0x0024ed0d,"\"3009 0 cPwV #COMMERCIAL#Speaking Mind Russia\"\nT%cKNpOkmEpXFNAQ9CbdpZ4WngYqRst6iHC!CMs9Ec!JbBNEz@elrGNSzG37\nUK5fP8OqnxGXZaPAEks59SByGUlO$4MXgF$P@FbxqqbmHti#\nSi!7AP$h!r4nwVVFqJ4j5T##\n");
		getVoicesList();
	}
 
	@Override
	public void init() {
		TTS.setLog(true);
		SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);		    			
		
	    String[] voicesList = getVoicesList();
    	for (String voice : voicesList) {
            String state=settings.getString(voice, "0");
            if (isDebug) {
                state="4";
            }
    		setVoiceState(voice, state);
	    }
	}

	public boolean isNeedDownload() {
		boolean res=true;		
    	for (ArrayList<String> voiceAr : voicesData) {
    		if (voiceAr.get(4).equals("3")||voiceAr.get(4).equals("4")) {
    	    	return false;
    		}
	    }
		return res;
	}

	@Override
	public boolean onDownloadSuccess(String voiceCode, String downloadedPath) {
		try {
			InputStream is = new FileInputStream(downloadedPath);
			ZipInputStream zis = new ZipInputStream(new BufferedInputStream(is));
			try {
			     ZipEntry ze;
			     while ((ze = zis.getNextEntry()) != null) {
			    	 int size;
		             byte[] buffer = new byte[2048];
		             if (ze.getName().charAt(ze.getName().length()-1)==File.separatorChar) {
		            	 File dirToSave = new File(VOCES_DIR + ze.getName());
		            	 dirToSave.mkdirs();
		             } else {
			             FileOutputStream fos = new FileOutputStream(VOCES_DIR + ze.getName());
			             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length);			 
			             while ((size = zis.read(buffer, 0, buffer.length)) != -1) {
			            	 bos.write(buffer, 0, size);
			             }
			             bos.flush();
			             bos.close();
		             }
			     }
			 } finally {
			     zis.close();
			 }
			File file=new File(downloadedPath);
	    	file.delete();
    		for (ArrayList<String> voiceAr : voicesData) {
    			if (voiceAr.get(4).equals(voiceCode)) {
    	    		setVoiceState(voiceCode, "3");
    			}
    		}

	    	String[] voicesList = getVoicesList();
	    	if (voicesList.length>0) {
	    		if (getState()!=STATE_READY) setState(STATE_READY);
				sendLangListChanged();
	    	}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public void speak(String text, String lang) {
		speak(text, lang, 0);
	}
	
	public void speak(String text, String lang, long prePos) {
		handler.removeCallbacks(autocompleteRunnable);
		lastPos=0;
		readedBefore=prePos;
		boolean langFound=false;
		for (ArrayList<String> voiceAr: voicesData) {
			Locale locale=new Locale(voiceAr.get(3));
			voiceAr.set(0, locale.getDisplayLanguage(locale));
		}
    	for (ArrayList<String> voiceAr: voicesData) {
    		if (voiceAr.get(3).compareToIgnoreCase(lang)==0&&voiceAr.get(4).equals("4")) {
    			if (!TTS.getLanguage().equals(voiceAr.get(2))) 
    				TTS.load(voiceAr.get(2));
    			langFound=true;
    			break;
    		}
    	}
    	
		if (langFound) {
			handler.postDelayed(autocompleteRunnable, 10*1000);
	    	isRead=true;
	    	needSendCompleteRead=true;
	    	//TTS.setPitch(100);
			TTS.setSpeechRate(speech_rate);
			readText=text;
			readLang=lang;
			TTS.speak(text);
			//Calendar c = Calendar.getInstance();
			//TTS.synthesizeToFile(text, Environment.getExternalStorageDirectory() + File.separator + "demo-"+c.get(Calendar.SECOND)+".wav");
		} else {
			sendUnsupportedLang(lang);
		}
	}

	@Override
	public void stopSpeak() {
        handler.removeCallbacks(autocompleteRunnable);
		needSendCompleteRead=false;
    	TTS.stop();
    	isRead=false;
	}

	@Override
	public void pauseSpeak() {
		handler.removeCallbacks(autocompleteRunnable);
        needSendCompleteRead=false;
		TTS.stop();
    	//TTS.pause();    
	}

	@Override
	public void resume() {
    	//TTS.resume();
    	for (long pos=lastPos;pos>=0;pos--) {
    		//if (readText.charAt((int) pos)=='.') {
    		if (readText.charAt((int) pos)=='.'||pos==0) {
                if (pos==0) pos-=1;
                readText=readText.substring((int) pos+1);
                speak(readText, readLang, readedBefore+pos+1);
                break;
            }
    	}
    	
    	handler.postDelayed(autocompleteRunnable, 10*1000);
	}

	@Override
	public boolean isSpeak() {
		return isRead;
	}

	@Override
    public void unload() {
		stop();
		TTS.shutdown();
	}

    public String[] getVoicesList() {
		String[] voiceDirPaths = {VOCES_DIR};
	    String[] voicesList = TTS.getVoicesList(voiceDirPaths);
    	return voicesList;
    }
    
	public void ttsevents(long type,long param1,long param2,long param3,long param4) {
		handler.removeCallbacks(autocompleteRunnable);
		//Log.i("[---acapela event---]", " "+type);
		if (type == acattsandroid.EVENT_TEXT_START) {
			//Log.i(TAG, "Text " + param1 + " started");
			handler.postDelayed(autocompleteRunnable, 5*1000);
		} else if (type == acattsandroid.EVENT_TEXT_END) {
			//Log.i("[---state---]", "Text " + param1 + " processed" + isRead);
			handler.postDelayed(autocompleteRunnable, 5*1000);
		} else if (type == acattsandroid.EVENT_WORD_POS) {
			if ((int)(param1+param2) <= readText.length() && param2 > 0) {
	    		//Log.i("[---pos---]", " pos : " + param1 + " - len : " + param2 + " - sampval : " + param3 );
	    		sendReadPosition((int)(param1+param2+readedBefore), (int)(readText.length()+readedBefore));
	    		lastPos=param1;    		
	    		handler.postDelayed(autocompleteRunnable, 5*1000);
			}
		} else if (type == acattsandroid.EVENT_AUDIO_END) {
			//Log.i("[---state---]", "Text " + param1 + " processed_all" + isRead);
			if (needSendCompleteRead) sendCompleteRead();
        }
	}

	@Override
	public String getDownloadFileByCode(String voiceCode) {
		for (ArrayList<String> voiceAr: voicesData) {
			if (voiceCode.equals(voiceAr.get(2))) {
				if (voiceAr.get(4).equals("1")) {
					if (voiceFiles.containsKey(voiceCode)) {
						return voiceFiles.get(voiceCode);
					}
				}
				break;
			}
		}
		return "";
	}

	@Override
	public ArrayList<ArrayList<String>> getVoicesData() {
		return voicesData;
	}

	@Override
	public ArrayList<String> setVoiceState(String code, String string) {
		SharedPreferences settings = mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = settings.edit();
		ArrayList<String> voice=null;
		boolean sendReady=false;
		for (ArrayList<String> voiceAr: voicesData) {
			if (code.equals(voiceAr.get(2))) {
				if (string.equals("1")) {
					String [] voices=getVoicesList();
					for (String v : voices) {
						if (v.equals(code)) {
							string="3";
							sendReady=true;
						}
					}
				}
				voiceAr.set(4, string);
				voice=voiceAr;
				if (!string.equals("2")) {
					editor.putString(code, string);
				}
				break;
			}
		}
		if (isSingle()) {
			if (voice!=null&&string.equals("4")) {
				for (ArrayList<String> voiceAr: voicesData) {
					if (voiceAr.get(3).equals(voice.get(3))&&!code.equals(voiceAr.get(2))&&voiceAr.get(4).equals("4")) {
						voiceAr.set(4, "3");
						editor.putString(voiceAr.get(2), "3");
					}
				}
			}
			boolean deffound=false;
			if (voice!=null&&string.equals("3")) {
				for (ArrayList<String> voiceAr: voicesData) {
					if (voiceAr.get(3).equals(voice.get(3))&&voiceAr.get(4).equals("4")) {
						deffound=true;
						break;
					}
				}
				if (!deffound) {
					voice.set(4, "4");
					editor.putString(code, "4");
				}
			}
		}
		editor.commit();
		if (sendReady) {
			if (getState()!=STATE_READY) setState(STATE_READY);
		}
		return voice;
	}

	@Override
	public ArrayList<String> getLangList() {
		ArrayList<String> langList=new ArrayList<String>();
		for (ArrayList<String> voiceAr : voicesData) {
			if (voiceAr.get(4).equals("4")) {
				langList.add(voiceAr.get(3));
			}
		}
		return langList;
	}

	@Override
	public String getSKUForVoice(String code) {
		return voicesSKUs.get(code);
	}

	@Override
	public void fixVoicesStates(ArrayList<String> purchaseSKUs) {	
		String[] voicesList = getVoicesList();
		for (ArrayList<String> voiceAr : voicesData) {
			String code=voiceAr.get(2);
			String sku=voicesSKUs.get(code);
			if (purchaseSKUs.contains(sku)) {
				if (voiceAr.get(4).equals("0")) {
					boolean found=false;
			    	for (String voice : voicesList) {
			    		if (voice.equals(code)) {
							setVoiceState(code, "3");
							found=true;
							break;
			    		}
			    	}	
			    	if (!found) setVoiceState(code, "1");
				} else if (voiceAr.get(4).equals("3")){
				} else if (voiceAr.get(4).equals("4")){
				} else { 	
			    	for (String voice : voicesList) {
			    		if (voice.equals(code)) {
							setVoiceState(code, "3");
							break;
			    		}
			    	}
				}
			} else {
				if (!voiceAr.get(4).equals("0")) {
					if (!isDebug) setVoiceState(code, "0");
				}
			}
		}
			    			
		for (ArrayList<String> voiceAr : voicesData) {
			String code=voiceAr.get(2);
    		setVoiceState(code, voiceAr.get(4));
	    }
		sendLangListChanged();
		if (isNeedDownload()) {
			setState(STATE_NEED_DOWNLOAD);
		} else {
			setState(STATE_READY);
		}
	}

	@Override
	public boolean containsDefaultVoice(String lang) {
    	for (ArrayList<String> voiceAr: voicesData) {
    		if (voiceAr.get(3).compareToIgnoreCase(lang)==0&&voiceAr.get(4).equals("4")) {
    			return true;
    		}
    	}
		return false;
	}

	@Override
	public void onVoiceDataChanged() {}
	
	@Override
	public boolean onNetworkStateChanged() {
		return false;
	}

	@Override
	public void setSpeechRate(float sr) {
		speech_rate = sr*100.f;
        TTS.setSpeechRate(speech_rate);
	}

}
