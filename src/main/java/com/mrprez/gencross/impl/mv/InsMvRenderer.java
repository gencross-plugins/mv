package com.mrprez.gencross.impl.mv;

import com.mrprez.gencross.Property;
import com.mrprez.gencross.renderer.Renderer;
import com.mrprez.gencross.value.DoubleValue;
import com.mrprez.gencross.value.Value;

public class InsMvRenderer extends Renderer {

	@Override
	public String displayValue(Value value) {
		if(value instanceof DoubleValue){
			double d = ((DoubleValue)value).getValue();
			int i = (int)d;
			double r = d - ((double)i);
			if(r==0){
				return ""+i;
			}else{
				return ""+i+"+";
			}
		}
		return value.toString();
	}

	@Override
	public String displayValue(Property property) {
		return displayValue(property.getValue());
	}

	
	
	

}
