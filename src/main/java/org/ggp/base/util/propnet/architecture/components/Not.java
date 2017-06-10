package org.ggp.base.util.propnet.architecture.components;

import java.util.List;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{

	public Not() {
		value = false;
	}

	@Override
   	public boolean propmark(){
		Component c = getSingleInputC();
       	return !c.propmark();
    }

	@Override
	public void diffProp(boolean newValue) {
		value = !getSingleInputC().getValue();
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
		file.append("boolean next = ");
		file.append("!comps[" + comps.indexOf(getSingleInputC()) + "];\n");

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
		return toDot("invtriangle", "grey", "NOT");
	}
}