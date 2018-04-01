import java.util.*;
import java.io.*;

public class Linker{

	private String fileName;
    private LinkedHashMap<String,Integer> symbolTableGlobal =  new LinkedHashMap<String,Integer>();
    private LinkedHashMap<String,Integer> symbolTableModule =  new LinkedHashMap<String,Integer>();
    private LinkedHashMap<String,String> errorMap = new LinkedHashMap<String,String>();
    private ArrayList<String> warnings = new ArrayList<>();
    private ArrayList<String> memoryMap = new ArrayList<>();
    private LinkedHashMap<String,String> memoryMapGlobal = new LinkedHashMap<String,String>();

    private void firstPass() throws Exception{
    	Scanner fileReader = new Scanner(new File(fileName));
    	int moduleOffset = 0;
    	int numberOfModules = fileReader.nextInt();
    	HashSet<String> multiplyDefinedSymbols = new HashSet<>();
    	HashSet<String> symbolsExceedingModule = new HashSet<>();
    	for(int module = 0; module < numberOfModules; module++){
    		int numDef = fileReader.nextInt();
    		LinkedHashMap<String,Integer> symbolsDefined = new LinkedHashMap<String,Integer>();
    		for(int def = 0; def < numDef; def++){
    			String symbol = fileReader.next();
    			int relativeAddress = fileReader.nextInt();
    			if(symbolsDefined.containsKey(symbol) || symbolTableGlobal.containsKey(symbol)){
    				multiplyDefinedSymbols.add(symbol);
    			}else{
    				symbolsDefined.put(symbol,relativeAddress);
    			}
    		}
    		int numUses = fileReader.nextInt();
    		HashSet<String> usesDeclared = new HashSet<>();
    		for(int use = 0; use < numUses; use++){
    			usesDeclared.add(fileReader.next());
    		}
    		int moduleSize = fileReader.nextInt();
    		for(String symbol:symbolsDefined.keySet()){
    			if(symbolsDefined.get(symbol)>=moduleSize){
    				symbolsExceedingModule.add(symbol);
    				symbolTableGlobal.put(symbol,moduleOffset);
    			}
    			else{
    				symbolTableGlobal.put(symbol,moduleOffset+symbolsDefined.get(symbol));
    			}
    			symbolTableModule.put(symbol,module);
    		}
    		moduleOffset += moduleSize;
    		for(int instructions = 0;instructions < moduleSize; instructions++){
    			String instructionType = fileReader.next();
    			int instruction = fileReader.nextInt();
    		}
    	}
    	System.out.println("---------------");
		System.out.println("Symbol Table - ");
		for(String symbol:symbolTableGlobal.keySet()){
			String toPrint = symbol+" = "+symbolTableGlobal.get(symbol);
			if(multiplyDefinedSymbols.contains(symbol)){
				toPrint += " Error : This variable is multiply defined; first value used.";
			}
			if(symbolsExceedingModule.contains(symbol)){
				toPrint += " Error : Definition exceeds the module size; zero (relative) used.";	
			}
			System.out.println(toPrint);
		}
		System.out.println("---------------");
    }

    private void secondPass() throws Exception{
    	Scanner fileReader = new Scanner(new File(fileName));
    	int moduleOffset = 0;
    	int numberOfModules = fileReader.nextInt();

    	LinkedHashMap<String,Boolean> globalyUsedSymbols = new LinkedHashMap<String,Boolean>();
    	for(String symbol:symbolTableGlobal.keySet()){
    		globalyUsedSymbols.put(symbol,false);
    	}

		System.out.println("Memory Map - ");

    	for(int module = 0; module < numberOfModules; module++){
    		System.out.println('+'+Integer.toString(moduleOffset));
    		int numDef = fileReader.nextInt();
    		LinkedHashMap<String,Integer> symbolsDefined = new LinkedHashMap<String,Integer>();
    		for(int def = 0; def < numDef; def++){
    			String symbol = fileReader.next();
    			int relativeAddress = fileReader.nextInt();
    		}
    		int numUses = fileReader.nextInt();
    		LinkedHashMap<Integer,String> usesDeclared = new LinkedHashMap<Integer,String>();
    		LinkedHashMap<String,Boolean> usesUsed = new LinkedHashMap<String,Boolean>();
    		for(int use = 0; use < numUses; use++){
    			String symbolUse = fileReader.next();
    			usesDeclared.put(use,symbolUse);
    			usesUsed.put(symbolUse,false);
    		}
    		int moduleSize = fileReader.nextInt();
    		for(int instructions = 0;instructions < moduleSize; instructions++){
    			String instructionType = fileReader.next();
    			String instruction = fileReader.next();
    			int address = Integer.parseInt(instruction.substring(1));
    			String finalMemoryMapEntry = instructionType+' '+instruction;
    			String toPrint = Integer.toString(instructions)+" :   "+instructionType+' '+instruction;
    			String errorMessage = " ";
    			switch(instructionType){
    				case "A" : 
    							if(address < 200){
    								finalMemoryMapEntry = "   :   "+instruction;
    								memoryMapGlobal.put(instructionType+' '+instruction,instruction);
    							}else{
    								finalMemoryMapEntry = "   :   "+instruction.charAt(0)+"000";
    								memoryMapGlobal.put(instructionType+' '+instruction,instruction.charAt(0)+"000");
    								errorMessage = " Error : Absolute address exceeds machine size; zero used.";
    							}
    							break;
    				case "I" : 
    							finalMemoryMapEntry = "   :   "+instruction;
    							memoryMapGlobal.put(instructionType+' '+instruction,instruction);
    							break;
    				case "R" : 
    							if(address < 200 && address < moduleSize){
    								String finalAddress = Integer.toString(address+moduleOffset);
    								String finalInstruction = instruction.charAt(0)+("000" + finalAddress).substring(finalAddress.length());
    								memoryMapGlobal.put(instructionType+' '+instruction,finalInstruction);
    								finalMemoryMapEntry = "   :   "+finalInstruction;
    							}else{
    								finalMemoryMapEntry = "   :   "+instruction.charAt(0)+"000";
    								memoryMapGlobal.put(instructionType+' '+instruction,instruction.charAt(0)+"000");
    								errorMessage = " Error : Relative address exceeds module size; zero used.";
    							}
    							break;
    				case "E" : 
    							if(address > numUses){
    								finalMemoryMapEntry = "   :   "+instruction;
    								memoryMapGlobal.put(instructionType+' '+instruction,instruction);
    								errorMessage = " Error : External address exceeds length of use list; treated as immediate.";
    							}
    							else{
    								if(usesDeclared.containsKey(address)){
	    								if(symbolTableGlobal.containsKey(usesDeclared.get(address))){
	    									String finalAddress = Integer.toString(symbolTableGlobal.get(usesDeclared.get(address)));
	    									String finalInstruction = instruction.charAt(0)+("000" + finalAddress).substring(finalAddress.length());
	    									memoryMapGlobal.put(instructionType+' '+instruction,finalInstruction);
	    									finalMemoryMapEntry = "   :   "+finalInstruction;
	    									globalyUsedSymbols.put(usesDeclared.get(address),true);
	    									usesUsed.put(usesDeclared.get(address),true);
	    								}else{
	    									finalMemoryMapEntry = "   :   "+instruction.charAt(0)+"000";
	    									memoryMapGlobal.put(instructionType+' '+instruction,instruction.charAt(0)+"000");
	    									errorMessage = " Error : "+usesDeclared.get(address)+" is not defined; zero used.";
	    								}
	    							}else{
	    								finalMemoryMapEntry = "   :   "+instruction;
	    								memoryMapGlobal.put(instructionType+' '+instruction,instruction);
	    								errorMessage = " Error : External address not in use list; treated as immediate.";
	    							}	
    							}
    							break;
    				default : 	
    							finalMemoryMapEntry = "   :   "+instruction;
    							errorMessage = " Error : Invalid instruction.";
    							memoryMapGlobal.put(instructionType+' '+instruction,instruction);
    							break;
    			}
    			memoryMap.add(finalMemoryMapEntry);
    			System.out.println(toPrint+finalMemoryMapEntry+errorMessage);
    		}
    		moduleOffset += moduleSize;
    		for(String symbol:usesUsed.keySet()){
    			if(!usesUsed.get(symbol) && globalyUsedSymbols.containsKey(symbol)){
    				String warning = "Warning : In module "+Integer.toString(module)+" "+symbol+" appeared in the use list but was not actually used.";
    				warnings.add(warning);
    			}
    		}
    	}
    	for (String symbol:globalyUsedSymbols.keySet()){
    		if(!globalyUsedSymbols.get(symbol)){
    			String warning = "Warning : "+symbol+" was defined in module "+Integer.toString(symbolTableModule.get(symbol))+" but never used.";
    			warnings.add(warning);
    		}
    	}
    }

    private void printWarnings() throws Exception{
    	if(warnings.size() > 0){
    		System.out.println("---------------");
	    	System.out.println("Warnings - ");
	    	for(String warning:warnings){
	    		System.out.println(warning);
	    	}
    	}
    }

	private void run() throws Exception{
        firstPass();
        secondPass();
        printWarnings();
        System.out.println("---------------");
    }

	public Linker(String fileName){
        this.fileName = fileName;
        try{
            run();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

	public static void main(String[] args){
        if(args.length != 1)
            throw new IllegalArgumentException("Incorrect number of parameters. One needed : <Input File Name>");
        Linker linker = new Linker(args[0]);
    }
}