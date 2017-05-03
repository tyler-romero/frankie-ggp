package org.ggp.base.player.gamer.statemachine.frankie;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Serializer {
	public Serializer(){}

	public void serializeTrainer(FrankieTrainer ft){
		try {
	         FileOutputStream fileOut = new FileOutputStream("frankieTrainer.ser");
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(ft);
	         out.close();
	         fileOut.close();
	         System.out.println("Serialized data is saved in frankieTrainer.ser");
	    }
		catch(IOException i) {
			System.out.println("Failed to serialize data.");
	    }
	}

	public FrankieTrainer deserializeTrainier() {
		FrankieTrainer ft = null;
		try {
	        FileInputStream fileIn = new FileInputStream("frankieTrainer.ser");
	        ObjectInputStream in = new ObjectInputStream(fileIn);
	        ft = (FrankieTrainer) in.readObject();
	        in.close();
	        fileIn.close();
	     }
		catch(IOException i) {
			System.out.println("Failed to deserialize data. Creating new frankieTrainer Instance.");
			ft = new FrankieTrainer();
	        return ft;
	     }
		catch(ClassNotFoundException c) {
	        System.out.println("FrankieTrainer class not found");
	        c.printStackTrace();
	        return null;
	     }
		return ft;
	}
}