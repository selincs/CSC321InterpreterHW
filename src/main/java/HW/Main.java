package HW;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {
    //HashMaps used to store unique Key/Value pairs easily for each respective data type
    //variable name = String key
    //numeric value = assigned int or double value
    //Maps unique requirement allows for variable reassignment easily, only one key can exist of a type so
    //multiple declarations will simply update the existing key's value, handling type checking as well with
    //a map for each data type
    private static final Map<String, Integer> intVars = new HashMap<>();
    private static final Map<String, Double> doubleVars = new HashMap<>();

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(new File("src/main/java/HW/input.txt"))) {
            //While scanner has not read all lines, continue loop
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim(); //Trim extra white space from end of string
                if (!line.isEmpty()) {
                    //Removable printing of processed lines
                    //System.out.println(line);

                    //If line has text(is not empty), process it
                    processLine(line);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Error: File not found.");
        }
        System.out.println("Integers stored : ");
        intVars.forEach((key, value) -> System.out.print("[" + key + " = " + value + "], "));
        System.out.println("\nDoubles stored : ");
        doubleVars.forEach((key, value) -> System.out.print("[" + key + " = " + value + "], "));
    }

    private static void processLine(String line) {
        // Determine the type of line: variable declaration, assignment, or print statement
        if (line.startsWith("int") || line.startsWith("double")) {
            declareVariable(line);
        } else if (line.startsWith("print(")) {     //Print line encountered in text file
            printExpression(line);
        } else {        // Handle uninitialized assignments or reassignments
            assignVariable(line);
        }
    }

    //Handle int or double declaration
    private static void declareVariable(String line) {
        // Regex Pattern to parse variable declarations with optional initialization
        // Group 1 : Determine if Group 1 is int OR double
        // \s+ one or more, \s* = optional white spaces between "="
        // Group 2 : Variable name - A whole word starting with a letter of any case, underscores allowed, digits after
        // Group 3 : Accommodate negative numbers and decimals for doubles

        Pattern pattern =
                Pattern.compile("(int|double)\\s+([a-zA-Z_]\\w*)(?:\\s*=\\s*([-\\d.]+))?;");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            String type = matcher.group(1);
            String varName = matcher.group(2);
            String value = matcher.group(3);

            // Store variables in appropriate map based on type
            if (type.equals("int")) {
                intVars.put(varName, value != null ? Integer.parseInt(value) : 0);
            } else {
                doubleVars.put(varName, value != null ? Double.parseDouble(value) : 0.0);
            }
        } else {
            System.out.println("Syntax error in declaration: " + line);
        }
    }

    private static void assignVariable(String line) {
        // Regex Pattern to parse variable assignments
        // Group 1 : Variable name - A whole word made of any letters any case, underscores allowed
        // \\s*= Optional spaces allowed before = sign
        //Group 2 : Variable value - At least one positive or negative digit, decimals allowed
        // or word if assigning a variable's value by name to another variable
        Pattern pattern =
                Pattern.compile("([a-zA-Z_]\\w*)\\s*=\\s*([-\\d.]+|[a-zA-Z_]\\w*);?");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            // If input line matches pattern, group 1 = var Name, group 2 = value (numeric or assigned)
            String varName = matcher.group(1);
            String value = matcher.group(2);

            // Ensure variable exists before assignment, and check which map it should be assigned to
            if (intVars.containsKey(varName)) {
                if (intVars.containsKey(value)) {
                    intVars.put(varName, intVars.get(value));
                } else {
                    intVars.put(varName, Integer.parseInt(value));
                }
            } else if (doubleVars.containsKey(varName)) {
                if (doubleVars.containsKey(value)) {
                    doubleVars.put(varName, doubleVars.get(value));
                } else {
                    doubleVars.put(varName, Double.parseDouble(value));
                }
            } else {
                System.out.println("Error: Variable not declared - " + varName);
            }
        } else {
            System.out.println("Syntax error in assignment: " + line);
        }
    }

    private static void printExpression(String line) {
        // Regex to extract expression inside print()
        // Literal string capture of Pattern with a "print(" for line start
        //Group 1 :
        // Literal capture of "\);" string via "\\);" pattern

        //Print lines are never split up on multiple lines so, I can just capture each individual group
        //and do the math individually

        //Print line of equation
        System.out.println(line);
        Pattern pattern = Pattern.compile("print\\((.*?)\\);");
        Matcher matcher = pattern.matcher(line);
        if (matcher.matches()) {
            // Replace any white spaces with empty char in print statement group
            String expression = matcher.group(1).replaceAll("\\s+", "");
            try {
                evaluateExpression(expression);
            } catch (Exception e) {
                System.out.println("Error evaluating expression: " + expression);
            }
        } else {
            System.out.println("Syntax error in print statement: " + line);
        }
    }


    //Return double because double can store int values, but int cannot store doubles
    //Just cast it to an int at the return to guarantee it is printed an integer without trailing decimals
    private static void evaluateExpression(String expression) {
        //List to store all tokens found in while loop
        List<String> tokens = new ArrayList<>();
        // While Loop to extract inputs until no tokens remaining
        //Pattern captures groups of one or more digits with optional decimal point and optional decimal numbers
        // OR variable names OR math operators: +, -, *, /
        Matcher matcher = Pattern.compile("\\d+\\.?\\d*|[a-zA-Z_]\\w*|[+\\-*/]").matcher(expression);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        //set initial values of variables
        double result = 0;
        String operator = "+";
        boolean isInt = true;
        int i = 0; // set index to 0 to peek ahead at the next operation as we iterate through the tokens list

        //while there are remaining tokens
        while (i < tokens.size()) {
            String token = tokens.get(i);
            //if this token matches an addition or subtraction operator, update the operator to the current token
            if (token.matches("[+\\-]")) {
                operator = token;
            } else {
                //Convert token to a numeric value (double or int, but double data type can store both)
                //if it is not already a stored variable
                double value = getValue(token);
                //If the next operator is multiplication or division, do this first to preserve order of ops
                if (i + 1 < tokens.size() && tokens.get(i + 1).matches("[*/]")) {
                    // Push-back logic: Look ahead for higher precedence operators (mult + div)
                    String nextOp = tokens.get(i + 1);
                    i += 2;
                    double nextValue = getValue(tokens.get(i));
                    // Multiplication + Division have higher precedence, do these operations before addition/subtraction
                    switch (nextOp) {
                        case "*":
                            value *= nextValue;
                            break;
                        case "/":
                            value /= nextValue;
                            break;
                    }
                }

                // Now apply the previous addition or subtraction operation before the mult / div operation
                switch (operator) {
                    case "+":
                        result += value;
                        break;
                    case "-":
                        result -= value;
                        break;
                }

                //Check if token is an int or not by validating if token directly has a decimal
                //or if the token is stored as a key in the HashMap storing doubles
                if (token.contains(".") || (doubleVars.containsKey(token))){
                    isInt = false;
                }
            }
            //Increment index
            i++;
        }


        //If isInt is true, cast result to int before returning, else return in double format
        if (isInt) {
            int resultInt = (int) result;
            System.out.println("Result: " + resultInt + "\n");
            return;
        }
          System.out.println("Result: " + result + "\n");
    }

    // Helper method to retrieve variable values or parse numbers directly printed
    //If a HashMap contains a key of the token, return that maps value, or
    //if it is a regular number being printed check if it contains a decimal or not to determine data type
    private static double getValue(String token) {
        if (intVars.containsKey(token)) return intVars.get(token);
        if (doubleVars.containsKey(token)) return doubleVars.get(token);
        if (!(token.contains("."))) return Integer.parseInt(token);
        return Double.parseDouble(token);
    }
}
