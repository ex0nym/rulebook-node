package io.exonym.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;


public class IsoCountryCode {
	
	private HashMap<String, String> country = new HashMap<String, String>();
	private static final Logger logger = LogManager.getLogger(IsoCountryCode.class);

	public static final String[] ALL_COUNTRIES =
		{"AF", "\u00C5X",  "AL",  "DZ",  "AS",  "AD",  "AO",  "AI",  "AQ",  "AG", 
			"AR",  "AM",  "AW",  "AU",  "AT",  "AZ",  "BS",  "BH",  "BD",  "BB", 
			"BY",  "BE",  "BZ",  "BJ",  "BM",  "BT",  "BO",  "BA",  "BW",  "BV", 
			"BR",  "VG",  "IO",  "BN",  "BG",  "BF",  "BI",  "KH",  "CM",  "CA", 
			"CV",  "KY",  "CF",  "TD",  "CL",  "CN",  "HK",  "MO",  "CX",  "CC", 
			"CO",  "KM",  "CG",  "CD",  "CK",  "CR",  "CI",  "HR",  "CU",  "CY", 
			"CZ",  "DK",  "DJ",  "DM",  "DO",  "EC",  "EG",  "SV",  "GQ",  "ER", 
			"EE",  "ET",  "FK",  "FO",  "FJ",  "FI",  "FR",  "GF",  "PF",  "TF", 
			"GA",  "GM",  "GE",  "DE",  "GH",  "GI",  "GR",  "GL",  "GD",  "GP", 
			"GU",  "GT",  "GG",  "GN",  "GW",  "GY",  "HT",  "HM",  "VA",  "HN", 
			"HU",  "IS",  "IN",  "ID",  "IR",  "IQ",  "IE",  "IM",  "IL",  "IT", 
			"JM",  "JP",  "JE",  "JO",  "KZ",  "KE",  "KI",  "KP",  "KR",  "KW", 
			"KG",  "LA",  "LV",  "LB",  "LS",  "LR",  "LY",  "LI",  "LT",  "LU", 
			"MK",  "MG",  "MW",  "MY",  "MV",  "ML",  "MT",  "MH",  "MQ",  "MR", 
			"MU",  "YT",  "MX",  "FM",  "MD",  "MC",  "MN",  "ME",  "MS",  "MA", 
			"MZ",  "MM",  "NA",  "NR",  "NP",  "NL",  "AN",  "NC",  "NZ",  "NI", 
			"NE",  "NG",  "NU",  "NF",  "MP",  "NO",  "OM",  "PK",  "PW",  "PS", 
			"PA",  "PG",  "PY",  "PE",  "PH",  "PN",  "PL",  "PT",  "PR",  "QA", 
			"RE",  "RO",  "RU",  "RW",  "BL",  "SH",  "KN",  "LC",  "MF",  "PM", 
			"VC",  "WS",  "SM",  "ST",  "SA",  "SN",  "RS",  "SC",  "SL",  "SG", 
			"SK",  "SI",  "SB",  "SO",  "ZA",  "GS",  "SS",  "ES",  "LK",  "SD", 
			"SR",  "SJ",  "SZ",  "SE",  "CH",  "SY",  "TW",  "TJ",  "TZ",  "TH", 
			"TL",  "TG",  "TK",  "TO",  "TT",  "TN",  "TR",  "TM",  "TC",  "TV", 
			"UG",  "UA",  "AE",  "GB",  "US",  "UM",  "UY",  "UZ",  "VU",  "VE", 
			"VN",  "VI",  "WF",  "EH",  "YE",  "ZM",  "ZW"};
	
	protected IsoCountryCode() { //
		
		logger.debug("initializing Iso Country Code");
		country.put("AF","Afghanistan");
		country.put("\u00C5X","\u00C5ringland Islands");
		country.put("AL","Albania");
		country.put("DZ","Algeria");
		country.put("AS","American Samoa");
		country.put("AD","Andorra");
		country.put("AO","Angola");
		country.put("AI","Anguilla");
		country.put("AQ","Antarctica");
		country.put("AG","Antigua and Barbuda");
		country.put("AR","Argentina");
		country.put("AM","Armenia");
		country.put("AW","Aruba");
		country.put("AU","Australia");
		country.put("AT","Austria");
		country.put("AZ","Azerbaijan");
		country.put("BS","Bahamas");
		country.put("BH","Bahrain");
		country.put("BD","Bangladesh");
		country.put("BB","Barbados");
		country.put("BY","Belarus");
		country.put("BE","Belgium");
		country.put("BZ","Belize");
		country.put("BJ","Benin");
		country.put("BM","Bermuda");
		country.put("BT","Bhutan");
		country.put("BO","Bolivia (Plurinational State of)");
		country.put("BQ","Bonaire Sint Eustatius and Saba");
		country.put("BA","Bosnia and Herzegovina");
		country.put("BW","Botswana");
		country.put("BV","Bouvet Island");
		country.put("BR","Brazil");
		country.put("IO","British Indian Ocean Territory");
		country.put("BN","Brunei Darussalam");
		country.put("BG","Bulgaria");
		country.put("BF","Burkina Faso");
		country.put("BI","Burundi");
		country.put("CV","Cabo Verde");
		country.put("KH","Cambodia");
		country.put("CM","Cameroon");
		country.put("CA","Canada");
		country.put("KY","Cayman Islands");
		country.put("CF","Central African Republic");
		country.put("TD","Chad");
		country.put("CL","Chile");
		country.put("CN","China");
		country.put("CX","Christmas Island");
		country.put("CC","Cocos (Keeling) Islands");
		country.put("CO","Colombia");
		country.put("KM","Comoros");
		country.put("CG","Congo");
		country.put("CD","Congo (Democratic Republic of the)");
		country.put("CK","Cook Islands");
		country.put("CR","Costa Rica");
		country.put("CI","Coete d'Ivoire");
		country.put("HR","Croatia");
		country.put("CU","Cuba");
		country.put("CW","Curacao");
		country.put("CY","Cyprus");
		country.put("CZ","Czechia");
		country.put("DK","Denmark");
		country.put("DJ","Djibouti");
		country.put("DM","Dominica");
		country.put("DO","Dominican Republic");
		country.put("EC","Ecuador");
		country.put("EG","Egypt");
		country.put("SV","El Salvador");
		country.put("GQ","Equatorial Guinea");
		country.put("ER","Eritrea");
		country.put("EE","Estonia");
		country.put("ET","Ethiopia");
		country.put("FK","Falkland Islands (Malvinas)");
		country.put("FO","Faroe Islands");
		country.put("FJ","Fiji");
		country.put("FI","Finland");
		country.put("FR","France");
		country.put("GF","French Guiana");
		country.put("PF","French Polynesia");
		country.put("TF","French Southern Territories");
		country.put("GA","Gabon");
		country.put("GM","Gambia");
		country.put("GE","Georgia");
		country.put("DE","Germany");
		country.put("GH","Ghana");
		country.put("GI","Gibraltar");
		country.put("GR","Greece");
		country.put("GL","Greenland");
		country.put("GD","Grenada");
		country.put("GP","Guadeloupe");
		country.put("GU","Guam");
		country.put("GT","Guatemala");
		country.put("GG","Guernsey");
		country.put("GN","Guinea");
		country.put("GW","Guinea-Bissau");
		country.put("GY","Guyana");
		country.put("HT","Haiti");
		country.put("HM","Heard Island and McDonald Islands");
		country.put("VA","Holy See");
		country.put("HN","Honduras");
		country.put("HK","Hong Kong");
		country.put("HU","Hungary");
		country.put("IS","Iceland");
		country.put("IN","India");
		country.put("ID","Indonesia");
		country.put("IR","Iran (Islamic Republic of)");
		country.put("IQ","Iraq");
		country.put("IE","Ireland");
		country.put("IM","Isle of Man");
		country.put("IL","Israel");
		country.put("IT","Italy");
		country.put("JM","Jamaica");
		country.put("JP","Japan");
		country.put("JE","Jersey");
		country.put("JO","Jordan");
		country.put("KZ","Kazakhstan");
		country.put("KE","Kenya");
		country.put("KI","Kiribati");
		country.put("KP","Korea (Democratic People's Republic of)");
		country.put("KR","Korea (Republic of)");
		country.put("KW","Kuwait");
		country.put("KG","Kyrgyzstan");
		country.put("LA","Lao People's Democratic Republic");
		country.put("LV","Latvia");
		country.put("LB","Lebanon");
		country.put("LS","Lesotho");
		country.put("LR","Liberia");
		country.put("LY","Libya");
		country.put("LI","Liechtenstein");
		country.put("LT","Lithuania");
		country.put("LU","Luxembourg");
		country.put("MO","Macao");
		country.put("MK","Macedonia (the former Yugoslav Republic of)");
		country.put("MG","Madagascar");
		country.put("MW","Malawi");
		country.put("MY","Malaysia");
		country.put("MV","Maldives");
		country.put("ML","Mali");
		country.put("MT","Malta");
		country.put("MH","Marshall Islands");
		country.put("MQ","Martinique");
		country.put("MR","Mauritania");
		country.put("MU","Mauritius");
		country.put("YT","Mayotte");
		country.put("MX","Mexico");
		country.put("FM","Micronesia (Federated States of)");
		country.put("MD","Moldova (Republic of)");
		country.put("MC","Monaco");
		country.put("MN","Mongolia");
		country.put("ME","Montenegro");
		country.put("MS","Montserrat");
		country.put("MA","Morocco");
		country.put("MZ","Mozambique");
		country.put("MM","Myanmar");
		country.put("NA","Namibia");
		country.put("NR","Nauru");
		country.put("NP","Nepal");
		country.put("NL","Netherlands");
		country.put("NC","New Caledonia");
		country.put("NZ","New Zealand");
		country.put("NI","Nicaragua");
		country.put("NE","Niger");
		country.put("NG","Nigeria");
		country.put("NU","Niue");
		country.put("NF","Norfolk Island");
		country.put("MP","Northern Mariana Islands");
		country.put("NO","Norway");
		country.put("OM","Oman");
		country.put("PK","Pakistan");
		country.put("PW","Palau");
		country.put("PS","Palestine, State of");
		country.put("PA","Panama");
		country.put("PG","Papua New Guinea");
		country.put("PY","Paraguay");
		country.put("PE","Peru");
		country.put("PH","Philippines");
		country.put("PN","Pitcairn");
		country.put("PL","Poland");
		country.put("PT","Portugal");
		country.put("PR","Puerto Rico");
		country.put("QA","Qatar");
		country.put("RE","Reunion");
		country.put("RO","Romania");
		country.put("RU","Russian Federation");
		country.put("RW","Rwanda");
		country.put("BL","Saint Barth‚lemy");
		country.put("SH","Saint Helena Ascension and Tristan da Cunha");
		country.put("KN","Saint Kitts and Nevis");
		country.put("LC","Saint Lucia");
		country.put("MF","Saint Martin (French part)");
		country.put("PM","Saint Pierre and Miquelon");
		country.put("VC","Saint Vincent and the Grenadines");
		country.put("WS","Samoa");
		country.put("SM","San Marino");
		country.put("ST","Sao Tome and Principe");
		country.put("SA","Saudi Arabia");
		country.put("SN","Senegal");
		country.put("RS","Serbia");
		country.put("SC","Seychelles");
		country.put("SL","Sierra Leone");
		country.put("SG","Singapore");
		country.put("SX","Sint Maarten (Dutch part)");
		country.put("SK","Slovakia");
		country.put("SI","Slovenia");
		country.put("SB","Solomon Islands");
		country.put("SO","Somalia");
		country.put("ZA","South Africa");
		country.put("GS","South Georgia and the South Sandwich Islands");
		country.put("SS","South Sudan");
		country.put("ES","Spain");
		country.put("LK","Sri Lanka");
		country.put("SD","Sudan");
		country.put("SR","Suriname");
		country.put("SJ","Svalbard and Jan Mayen");
		country.put("SZ","Swaziland");
		country.put("SE","Sweden");
		country.put("CH","Switzerland");
		country.put("SY","Syrian Arab Republic");
		country.put("TW","Taiwan");
		country.put("TJ","Tajikistan");
		country.put("TZ","Tanzania, United Republic of");
		country.put("TH","Thailand");
		country.put("TL","Timor-Leste");
		country.put("TG","Togo");
		country.put("TK","Tokelau");
		country.put("TO","Tonga");
		country.put("TT","Trinidad and Tobago");
		country.put("TN","Tunisia");
		country.put("TR","Turkey");
		country.put("TM","Turkmenistan");
		country.put("TC","Turks and Caicos Islands");
		country.put("TV","Tuvalu");
		country.put("UG","Uganda");
		country.put("UA","Ukraine");
		country.put("AE","United Arab Emirates");
		country.put("GB","United Kingdom of Great Britain and Northern Ireland");
		country.put("US","United States of America");
		country.put("UM","United States Minor Outlying Islands");
		country.put("UY","Uruguay");
		country.put("UZ","Uzbekistan");
		country.put("VU","Vanuatu");
		country.put("VE","Venezuela (Bolivarian Republic of)");
		country.put("VN","Vietnam");
		country.put("VG","Virgin Islands (British)");
		country.put("VI","Virgin Islands (U.S.)");
		country.put("WF","Wallis and Futuna");
		country.put("EH","Western Sahara");
		country.put("YE","Yemen");
		country.put("ZM","Zambia");
		country.put("ZW","Zimbabwe");

	}
	
	public synchronized boolean isCountryCode(String isoCode){
		return country.containsKey(isoCode.toUpperCase());
		
	}
	
	public synchronized String getCountry(String isoCode){
		return country.get(isoCode.toUpperCase());
		
	}
	
	static {
		try {
			instance = new IsoCountryCode();
			
		} catch (Exception e) {
			e.printStackTrace();
			
		}
	} 
	
	private static IsoCountryCode instance;
	
	public static synchronized IsoCountryCode getInstance(){
		return instance; 
	}
}
