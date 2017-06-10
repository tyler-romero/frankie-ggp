package org.ggp.base.util.propnet.architecture.components;

import java.util.List;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class Transition extends Component
{
	@Override
   	public boolean propmark(){
		Component c = getSingleInputC();
		return c.propmark();
    }

    @Override
    public void diffProp(boolean newValue) {
        value = newValue;
        if (value != last) {
			last = value;
			for (Component c : getOutputC()){
				c.diffProp(value);
			}
		}
    }

    @Override
	public void clear() {
    	last = false;
		value = false;
		isValid = false;
	}

    @Override
	public void makeMethod(StringBuilder file, List<Component> comps) {
    	file.append("private void propagate" + comps.indexOf(this) + "(boolean newValue){\n");
		file.append("if (newValue != comps[" + comps.indexOf(this) + "]){\n");
		file.append("comps[" + comps.indexOf(this) + "] = newValue;\n");
		for (Component c : getOutputC()) {
			file.append("propagate" + comps.indexOf(c) + "(newValue);\n");
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
        return toDot("box", "grey", "TRANSITION");
    }
}