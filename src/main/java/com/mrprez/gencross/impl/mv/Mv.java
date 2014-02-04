package com.mrprez.gencross.impl.mv;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.PropertiesList;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.history.ConstantHistoryFactory;
import com.mrprez.gencross.history.FreeHistoryFactory;
import com.mrprez.gencross.history.HistoryFactory;
import com.mrprez.gencross.history.HistoryItem;
import com.mrprez.gencross.history.HistoryUtil;
import com.mrprez.gencross.history.ProportionalHistoryFactory;
import com.mrprez.gencross.util.PersonnageUtil;
import com.mrprez.gencross.value.DoubleValue;
import com.mrprez.gencross.value.IntValue;
import com.mrprez.gencross.value.Value;

public class Mv extends Personnage {
	
	@Override
	public void calculate() {
		super.calculate();
		if(phase.equals("Choix Superieur")){
			if(getProperty("Superieur").getValue().toString().equals("")){
				errors.add("Vous devez choisir un supérieur");
			}
		}else if(phase.equals("Creation")){
			calculateCaracValueError();		
			checkPAInteval("Caracteristiques",20,50);
			checkPATalent();
			checkPAPouvoirs();
			checkPAInteval("PP",2,10);
			checkLimitation();
		}
	}
	
	
	
	private void addPouvoirsExclusifs(){
		String superieur = getProperty("Superieur").getValue().toString();
		superieur = superieur.substring(0,superieur.indexOf(" - "));
		Map<String, String> pouvoirsExclusifs = appendix.getSubMap("pouvoirExclusif."+superieur);
		for(String pouvoirExclusif : pouvoirsExclusifs.values()){
			PropertiesList pouvoirList = getProperty("Pouvoirs").getSubProperties();
			Property newPouvoir = new Property(pouvoirExclusif,getProperty("Pouvoirs"));
			if("oui".equals(appendix.getProperty(pouvoirExclusif+".invariable", "non"))){
				int cout = Integer.parseInt(appendix.getProperty(pouvoirExclusif+".cout","3"));
				newPouvoir.setHistoryFactory(new ConstantHistoryFactory("PA", cout));
			}else{
				DoubleValue newValue = new DoubleValue(1.0);
				newValue.setOffset(0.5);
				newPouvoir.setValue(newValue);
				newPouvoir.setMin();
				newPouvoir.setMax(new DoubleValue(2.5));
				int cout = Integer.parseInt(appendix.getProperty(pouvoirExclusif+".cout","3"));
				newPouvoir.setHistoryFactory(new ProportionalHistoryFactory("PA", cout));
			}
			newPouvoir.setRenderer(new InsMvRenderer());
			pouvoirList.getOptions().put(newPouvoir.getFullName(), newPouvoir);
		}
	}
	
	
	private void changePouvoirsCosts(){
		String superieur = getProperty("Superieur").getValue().toString();
		superieur = superieur.substring(0,superieur.indexOf(" - "));
		Map<String, String> coutsReduits = appendix.getSubMap("coutreduit."+superieur);
		for(String key : coutsReduits.keySet()){
			String pouvoirName = key.substring(key.lastIndexOf('.')+1).replaceAll("_", " ");
			Property pouvoir = getProperty("Pouvoirs").getSubProperties().getOptions().get(pouvoirName);
			HistoryFactory historyFactory = pouvoir.getHistoryFactory();
			Map<String, String> args = new HashMap<String, String>();
			if(historyFactory instanceof ConstantHistoryFactory){
				args.put("cost", coutsReduits.get(key));
			}else{
				args.put("factor", coutsReduits.get(key));
			}
			historyFactory.setArgs(args);
		}
	}



	private void checkPAInteval(String source, int min, int max){
		int spendPA = HistoryUtil.sumHistory(history, source+"#[^#]*", "PA");
		spendPA = spendPA + HistoryUtil.sumHistory(history, source, "PA");
		if(spendPA<min || spendPA>max){
			errors.add("Vous devez dépenser entre "+min+" et "+max+" PA en "+source);
		}
	}
	
	private void checkPAPouvoirs(){
		int spendPA = HistoryUtil.sumHistory(history, "Pouvoirs#[^#]*", "PA");
		spendPA = spendPA + HistoryUtil.sumHistory(history, "Avantages#[^#]*", "PA");
		if(spendPA<20 || spendPA>35){
			errors.add("Vous devez dépenser entre 20 et 35 PA en pouvoirs ou avantages");
		}
	}
	
	private void checkPATalent(){
		int spendPA = HistoryUtil.sumHistoryOfSubTree(history, getProperty("Talents principaux"), "PA");
		spendPA = spendPA + HistoryUtil.sumHistoryOfSubTree(history, getProperty("Talents exotiques"), "PA");
		spendPA = spendPA + HistoryUtil.sumHistory(history, "PA en talents secondaires", "PA");
		if(spendPA<25 || spendPA>50){
			errors.add("Vous devez dépenser entre 25 et 50 PA en Talents");
		}
	}
	
	private void checkLimitation(){
		if(getProperty("Limitations").getSubProperties().size()>1){
			errors.add("Vous ne pouvez avoir plus d'une limitation");
		}
	}
	
	private void calculateCaracValueError(){
		Iterator<Property> it = this.getProperty("Caracteristiques").iterator();
		int compte = 0;
		while(it.hasNext()){
			Property carac = it.next();
			if(((DoubleValue)carac.getValue()).getValue()==1.5){
				compte++;
			}
		}
		if(compte>1){
			errors.add("Vous ne pouvez avoir qu'une caractéristique à 1+");
		}
	}
	
	public Boolean checkCaracChangement(Property carac, Value newValue){
		Iterator<Property> it = getProperty("Talents principaux").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				if(!checkBaseValue(talent, newValue.getDouble(), carac.getValue().getDouble())){
					return false;
				}
			}
		}
		it = getProperty("Talents secondaires").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				if(!checkBaseValue(talent, newValue.getDouble(), carac.getValue().getDouble())){
					return false;
				}
			}
		}
		it = getProperty("Talents exotiques").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				if(!checkBaseValue(talent, newValue.getDouble(), carac.getValue().getDouble())){
					return false;
				}
			}
		}
		if(carac.getAbsoluteName().equals("Caracteristiques#Foi/Chance") || carac.getAbsoluteName().equals("Caracteristiques#Volonté")){
			int currentPP = getProperty("PP").getValue().getInt();
			int oldPPstartValue = (int) (getProperty("Caracteristiques#Foi/Chance").getValue().getDouble()
					+getProperty("Caracteristiques#Volonté").getValue().getDouble());
			int newPPstartValue = (int) (getProperty("Caracteristiques#Foi/Chance").getValue().getDouble()
					+getProperty("Caracteristiques#Volonté").getValue().getDouble()
					-carac.getValue().getDouble()
					+newValue.getDouble());
			if(currentPP - oldPPstartValue + newPPstartValue > getProperty("PP").getMax().getInt()){
				actionMessage = "Vous ne pouvez avoir plus de "+getProperty("PP").getMax().getInt()+" PP";
				return false;
			}
		}
		return true;
	}
	
	private boolean checkBaseValue(Property talent, Double newCaracValue, Double oldCaracValue){
		double oldStart = Math.floor(oldCaracValue)/2.0;
		double newStart = Math.floor(newCaracValue)/2.0;
		if(talent.getValue()!=null){
			if(talent.getValue().getDouble()-oldStart+newStart > talent.getMax().getDouble()){
				actionMessage = talent.getFullName()+" ne peut dépasser "+new InsMvRenderer().displayValue(talent.getMax());
				return false;
			}
		}
		if(talent.getSubProperties()!=null){
			Iterator<Property> it = talent.getSubProperties().iterator();
			while(it.hasNext()){
				if(!checkBaseValue(it.next(), newCaracValue, oldCaracValue)){
					return false;
				}
			}
		}
		return true;
	}
	
	public void changeCarac(Property carac, Value oldValue){
		Iterator<Property> it = getProperty("Talents principaux").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				changeBaseValue(talent,(DoubleValue)oldValue, (DoubleValue)carac.getValue());
			}
		}
		it = getProperty("Talents exotiques").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				changeBaseValue(talent,(DoubleValue)oldValue, (DoubleValue)carac.getValue());
			}
		}
		it = getProperty("Talents exotiques").getSubProperties().getOptions().values().iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				changeBaseValue(talent,(DoubleValue)oldValue, (DoubleValue)carac.getValue());
				Map<String, String> args = ((ProportionalHistoryFactory)talent.getHistoryFactory()).getArgs();
				args.put("startValue", talent.getValue().getString());
				((ProportionalHistoryFactory)talent.getHistoryFactory()).setArgs(args);
			}
		}
		it = getProperty("Talents secondaires").iterator();
		while(it.hasNext()){
			Property talent = it.next();
			if(appendix.getProperty("carac."+talent.getFullName(),"").equals(carac.getFullName())){
				changeBaseValue(talent,(DoubleValue)oldValue, (DoubleValue)carac.getValue());
			}
		}
	}
	
	public void changePPImpliedCarac(Property carac, Value oldValue){
		int oldPP = ((IntValue)getProperty("PP").getValue()).getValue();
		int newPPstartValue = (int) (getProperty("Caracteristiques#Foi/Chance").getValue().getDouble()
				+getProperty("Caracteristiques#Volonté").getValue().getDouble());
		int oldPPstartValue = (int) (getProperty("Caracteristiques#Foi/Chance").getValue().getDouble()
				+getProperty("Caracteristiques#Volonté").getValue().getDouble()
				-carac.getValue().getDouble()
				+oldValue.getDouble());
		getProperty("PP").setValue(Value.createValue(oldPP-oldPPstartValue+newPPstartValue));
		((IntValue)getProperty("PP").getMin()).setValue(newPPstartValue);
	}
	
	public void changeTalent(Property talent, Value oldValue){
		HistoryItem historyItem = history.get(history.size()-1);
		if(historyItem.getPointPool().equals("PA")){
			int cost = historyItem.getCost();
			pointPools.get("Points de talents secondaires").add(cost);
		}
	}
	
	protected void changeBaseValue(Property talent, DoubleValue oldCaracValue, DoubleValue newCaracValue){
		if(talent.getValue()!=null){
			double oldStart = Math.floor(oldCaracValue.getValue())/2.0;
			double newStart = Math.floor(newCaracValue.getValue())/2.0;
			talent.setValue(new DoubleValue(((DoubleValue)talent.getValue()).getValue()-oldStart+newStart));
			talent.getValue().setOffset(0.5);
			talent.setMin(new DoubleValue(newStart));
		}
		if(talent.getSubProperties()!=null && talent.getSubProperties().size()>0){
			changeBaseValue(talent.getSubProperties().get(0),oldCaracValue,newCaracValue);
		}
	}

	public void addSpecialite(Property newProperty){
		Property owner = (Property)newProperty.getOwner();
		if(owner.getValue()!=null){
			owner.getSubProperties().setFixe(true);
		}
		if(appendix.getProperty("carac."+owner.getFullName())!=null){
			String caracName = appendix.getProperty("carac."+owner.getFullName());
			Property carac = getProperty("Caracteristiques#"+caracName);
			newProperty.setValue(new DoubleValue(Math.floor(carac.getValue().getDouble())/2.0));
			newProperty.setMin();
			((DoubleValue)newProperty.getValue()).setOffset(0.5);
		}else{
			if(!owner.getName().equals("Langues")){
				newProperty.setValue(new DoubleValue(1.0));
				newProperty.setMin();
				((DoubleValue)newProperty.getValue()).setOffset(0.5);
			}
		}
	}
	
	public Boolean removeSpecialite(Property specialite){
		Property talent = (Property) specialite.getOwner();
		if(talent.getMin()!=null){
			if(specialite.getValue().getDouble()>talent.getMin().getDouble()){
				actionMessage = "Impossible de supprimer une spécialité supérieur à la valeur de base du talent";
				return false;
			}
		}else if(specialite.getValue()!=null && specialite.getValue().getDouble()>1.0){
			actionMessage = "Impossible de supprimer une spécialité supérieur à 1";
			return false;
		}
		specialite.setValue(Value.createValue(0.0));
		talent.getSubProperties().setFixe(false);
		return true;
	}
	
	public boolean changeTalentWithSpe(Property property, Value newValue){
		if(property.getSubProperties()==null){
			return true;
		}
		if(property.getSubProperties().size()==0){
			actionMessage = "Vous devez d'abord ajouter une spécialité";
			return false;
		}
		Property specialite = property.getSubProperties().get(0);
		if(newValue.getDouble()>specialite.getValue().getDouble()){
			actionMessage = "Le talent ne peut dépasser sa spécialité";
			return false;
		}
		return true;
	}
	
	public boolean changeSpe(Property specialite, Value newValue){
		Property talent = (Property) specialite.getOwner();
		if(talent.getValue()!=null && talent.getValue().getDouble()>newValue.getDouble()){
			actionMessage = "Le talent ne peut dépasser sa spécialité";
			return false;
		}
		return true;
	}
	
	public void passToGrade0(){
		PersonnageUtil.setMinRecursivly(this, "Caracteristiques");
		PersonnageUtil.setMinRecursivly(this, "Talents principaux");
		PersonnageUtil.setMinRecursivly(this, "PA en talents secondaires");
		PersonnageUtil.setMinRecursivly(this, "Talents secondaires");
		PersonnageUtil.setMinRecursivly(this, "Pouvoirs");
		PersonnageUtil.setMinRecursivly(this, "PP");
		PersonnageUtil.setMaxRecursivly(this, "Caracteristiques", new DoubleValue(5.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents principaux", new DoubleValue(7.5));
		PersonnageUtil.removeMaxRecursivly(this, "PA en talents secondaires");
		PersonnageUtil.setMaxRecursivly(this, "Talents secondaires", new DoubleValue(7.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents exotiques", new DoubleValue(7.5));
		PersonnageUtil.setMaxRecursivly(this, "Pouvoirs", new DoubleValue(2.5));
		getProperty("Limitations").setHistoryFactory(new FreeHistoryFactory("PA"));
		this.getPointPools().get("PA").setToEmpty(false);
	}
	
	public void passToGrade1(){
		getPointPools().get("Pouvoirs de grade").add(2);
		getProperty("Grade").setValue(new IntValue(1));
		PersonnageUtil.setMaxRecursivly(this, "Talents principaux", new DoubleValue(8.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents secondaires", new DoubleValue(8.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents exotiques", new DoubleValue(8.5));
		PersonnageUtil.setMaxRecursivly(this, "Pouvoirs", new DoubleValue(4.5));
		PersonnageUtil.setMaxRecursivly(this, "PP", new IntValue(25));
	}
	
	public void passToGrade2(){
		getPointPools().get("Pouvoirs de grade").add(2);
		getProperty("Grade").setValue(new IntValue(2));
		PersonnageUtil.setMaxRecursivly(this, "Caracteristiques", new DoubleValue(6.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents principaux", new DoubleValue(9.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents secondaires", new DoubleValue(9.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents exotiques", new DoubleValue(9.5));
		PersonnageUtil.setMaxRecursivly(this, "Pouvoirs", new DoubleValue(6.5));
		PersonnageUtil.setMaxRecursivly(this, "PP", new IntValue(40));
	}
	
	public void passToGrade3(){
		getPointPools().get("Pouvoirs de grade").add(2);
		getProperty("Grade").setValue(new IntValue(3));
		PersonnageUtil.setMaxRecursivly(this, "Caracteristiques", new DoubleValue(7.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents principaux", new DoubleValue(10.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents secondaires", new DoubleValue(10.5));
		PersonnageUtil.setMaxRecursivly(this, "Talents exotiques", new DoubleValue(10.5));
		PersonnageUtil.setMaxRecursivly(this, "Pouvoirs", new DoubleValue(8.5));
		PersonnageUtil.setMaxRecursivly(this, "PP", new IntValue(60));
	}
	
	public void passToCreationPhase(){
		getProperty("Caracteristiques").setEditableRecursivly(true);
		getProperty("Talents principaux").setEditableRecursivly(true);
		getProperty("PA en talents secondaires").setEditable(true);
		getProperty("Talents secondaires").setEditableRecursivly(true);
		getProperty("Talents exotiques").getSubProperties().setFixe(false);
		getProperty("Pouvoirs").getSubProperties().setFixe(false);
		getProperty("PP").setEditable(true);
		getProperty("Avantages").getSubProperties().setFixe(false);
		getProperty("Limitations").getSubProperties().setFixe(false);
		getProperty("Superieur").setEditable(false);
		addPouvoirsExclusifs();
		changePouvoirsCosts();
		this.getPointPools().get("PA").setToEmpty(true);
		
		// Ajout des pouvoirs de grade
		String superieur = getProperty("Superieur").getValue().toString();
		superieur = superieur.substring(0,superieur.indexOf(" - "));
		Collection<String> pouvoirsGrade = appendix.getSubMap("pouvoirGrade."+superieur+".").values();
		for(String domaine : pouvoirsGrade){
			Property pouvoir = new Property(domaine, getProperty("Pouvoirs de grade"));
			pouvoir.setValue(new IntValue(0));
			pouvoir.setMax(new IntValue(6));
			pouvoir.setMin();
			getProperty("Pouvoirs de grade").getSubProperties().add(pouvoir);
		}
		
		// Talents exotiques spécifiques au prince démon
		for(String talentName : appendix.getSubMap("talent_exotique."+superieur+".").values()){
			Property talent = getProperty("Talents exotiques").getSubProperties().getOptions().get(talentName);
			talent.getHistoryFactory().setPointPool("Points de talents secondaires");
		}
	}
	
	

}
