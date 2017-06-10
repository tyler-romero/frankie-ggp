package org.ggp.base.util.propnet.architecture.components;

import java.util.List;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Or class is designed to represent logical OR gates.
 */
@SuppressWarnings("serial")
public final class Or extends Component
{
	int numTrue = 0;

    @Override
   	public boolean propmark(){
       	for (Component c : getInputC()){
       		if (c.propmark())
       			return true;
   		}
       	return false;
    }

    @Override
    public void diffProp(boolean newValue)
    {
    	if(newValue) numTrue += 1;
    	else numTrue -= 1;
    	if(numTrue != 0) value = true;
    	else value = false;

        for (Component component : getInputC()) {
            if (component.getValue()) {
                value = true;
                break;
            }
        }
        if (value != last) {
			last = value;
			for (Component c : getOutputC()) {
				c.diffProp(value);
			}
		}
    }

    @Override
	public void clear() {
		value = false;
		last = false;
		numTrue = 0;
		isValid = false;
	}

    @Override
	public void makeMethod(StringBuilder file, List<Component> comps) {
    	file.append("private void propagate" + comps.indexOf(this) + "(boolean newValue){\n");
    	file.append("boolean next = ");
    	file.append("comps[" + comps.indexOf(getInputC()[0]) + "]");
    	for (int i = 1; i < getInputC().length; i ++) {
    		file.append(" || comps[" + comps.indexOf(getInputC()[i]) + "]");
    	}
    	file.append(";\n");

    	file.append("if (next != comps[" + comps.indexOf(this) + "]){\n");
		file.append("comps[" + comps.indexOf(this) + "] = next;\n");
		for (Component c : getOutputC()) {
			file.append("propagate" + comps.indexOf(c) + "(next);\n");
		}
		file.append("}\n");

    	file.append("}\n");
    }

    /**
     * @see org.ggp.base.util.propnet.architecture.Component#toString()
     */
    @Override
    public String toString()
    {
        return toDot("ellipse", "grey", "OR");
    }
}