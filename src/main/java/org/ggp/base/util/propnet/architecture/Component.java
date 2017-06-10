package org.ggp.base.util.propnet.architecture;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.components.Proposition;

/**
 * The root class of the Component hierarchy, which is designed to represent
 * nodes in a PropNet. The general contract of derived classes is to override
 * all methods.
 */

public abstract class Component implements Serializable
{
	public boolean isValid = false;
	protected boolean value = false;
	protected boolean last = false;

	private static final long serialVersionUID = 352524175700224447L;

	/** The inputs to the component. */
	private Set<Component> inputs;
	/** The outputs of the component. */
	private Set<Component> outputs;

	private Component[] input_array;
	private Component[] output_array;

	/**
	 * Creates a new Component with no inputs or outputs.
	 */
	public Component()
	{
		this.inputs = new HashSet<Component>();
		this.outputs = new HashSet<Component>();
	}

	public void crystalize() {
		input_array = new Component[inputs.size()];
		inputs.toArray(input_array);
		output_array = new Component[outputs.size()];
		outputs.toArray(output_array);
	}

	/**
	 * Adds a new input.
	 *
	 * @param input
	 *            A new input.
	 */
	public void addInput(Component input)
	{
		inputs.add(input);
	}

	public void flood(){
		if (isValid) return;
		isValid = true;
		for (Component c : getInputC()){
			c.flood();
		}
		for (Component c : getOutputC()){
			c.flood();
		}
	}

	public void removeInput(Component input)
	{
		inputs.remove(input);
	}

	public void removeOutput(Component output)
	{
		outputs.remove(output);
	}

	public void removeAllInputs()
	{
		inputs.clear();
	}

	public void removeAllOutputs()
	{
		outputs.clear();
	}

	public abstract void makeMethod(StringBuilder file, List<Component> comps);

	/**
	 * Adds a new output.
	 *
	 * @param output
	 *            A new output.
	 */
	public void addOutput(Component output)
	{
		outputs.add(output);
	}

	/**
	 * Getter method.
	 *
	 * @return The inputs to the component.
	 */
	public Set<Component> getInputs()
	{
		return inputs;
	}

	public Component[] getInputC() {
		return input_array;
	}

	public Component getSingleInputC() {
		if(input_array.length != 1){
			System.out.println("Input size is not 1 (it is size " + input_array.length + ")");
			if(this instanceof Proposition){
				System.out.println("This is a proposition with name " + ((Proposition)this).getName());
				if(((Proposition)this).base){
					System.out.println("This is a proposition is a base");
				}
			}
		}
		return input_array[0];
	}


	/**
	 * A convenience method, to get a single input.
	 * To be used only when the component is known to have
	 * exactly one input.
	 *
	 * @return The single input to the component.
	 */
	public Component getSingleInput() {
		assert inputs.size() == 1;
		return inputs.iterator().next();
	}

	/**
	 * Getter method.
	 *
	 * @return The outputs of the component.
	 */
	public Set<Component> getOutputs()
	{
		return outputs;
	}

	/**
	 * A convenience method, to get a single output.
	 * To be used only when the component is known to have
	 * exactly one output.
	 *
	 * @return The single output to the component.
	 */
	public Component getSingleOutput() {
		assert outputs.size() == 1;
		return outputs.iterator().next();
	}

	public Component[] getOutputC() {
		return output_array;
	}

	public Component getSingleOutputC() {
		assert output_array.length == 1;
		return output_array[0];
	}

	/**
	 * Returns the value of the Component.
	 *
	 * @return The value of the Component.
	 */
	public boolean getValue() {
		return value;
	}

	public abstract boolean propmark();
	public abstract void diffProp(boolean newValue);
	public abstract void clear();


	/**
	 * Returns a representation of the Component in .dot format.
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public abstract String toString();


	/**
	 * Returns a configurable representation of the Component in .dot format.
	 *
	 * @param shape
	 *            The value to use as the <tt>shape</tt> attribute.
	 * @param fillcolor
	 *            The value to use as the <tt>fillcolor</tt> attribute.
	 * @param label
	 *            The value to use as the <tt>label</tt> attribute.
	 * @return A representation of the Component in .dot format.
	 */
	protected String toDot(String shape, String fillcolor, String label)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("\"@" + Integer.toHexString(hashCode()) + "\"[shape=" + shape + ", value="+ value+", fillcolor=" + fillcolor + ", label=\"" + label + "\"]; ");
		for ( Component component : getOutputs() )
		{
			sb.append("\"@" + Integer.toHexString(hashCode()) + "\"->" + "\"@" + Integer.toHexString(component.hashCode()) + "\"; ");
		}

		return sb.toString();
	}

}