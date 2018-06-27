package net.lintim.io;

import net.lintim.exception.OutputFileException;
import net.lintim.solver.Constraint;
import net.lintim.solver.LinearExpression;
import net.lintim.solver.Model;
import net.lintim.solver.Variable;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;

/**
 * Class for writing LP files.
 */
public class LPWriter {

    /**
     * Write the given model to the given file. The file will be written in the lp-file format.
     * @param model the model to write
     * @param filename the file to write to
     */
    public static void writeProblem(Model model, String filename){
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
            writer.write("\\ LP file written by the LinTim solver abstraction\n");
            switch (model.getSense()) {
                case MINIMIZE:
                    writer.write("Minimize\n");
                    break;
                case MAXIMIZE:
                    writer.write("Maximize\n");
                    break;
            }
            writer.write(" ");
            LinearExpression objective = model.getObjective();
            for (Map.Entry<Variable, Double> objectiveEntries : objective.getEntries()) {
                writer.write(" " + formatVariable(objectiveEntries.getKey(), objectiveEntries.getValue()));
            }
            writer.write("\nSubject To\n");
            for (Constraint constraint : model.getConstraints()) {
                writer.write(" " + constraint.getName() + ":");
                for (Map.Entry<Variable, Double> entry : constraint.getExpression().getEntries()) {
                    writer.write(" " + formatVariable(entry.getKey(), entry.getValue()));
                }
                switch (constraint.getSense()) {
                    case LESS_EQUAL:
                        writer.write(" <= ");
                        break;
                    case EQUAL:
                        writer.write(" = ");
                        break;
                    case GREATER_EQUAL:
                        writer.write(" >= ");
                        break;
                }
                writer.write(constraint.getRhs() + "\n");
            }
            writer.write("Bounds\n");
            HashSet<Variable> binaryVariables = new HashSet<>();
            HashSet<Variable> integerVariables = new HashSet<>();
            for (Variable variable : model.getVariables()) {
                writer.write(" " + variable.getLowerBound() + " <= " + variable.getName() + " <= " + variable
                    .getUpperBound() + "\n");
                switch (variable.getType()) {
                    case BINARY:
                        binaryVariables.add(variable);
                        break;
                    case INTEGER:
                        integerVariables.add(variable);
                        break;
                }
            }
            if (!binaryVariables.isEmpty()) {
                writer.write("Binaries\n");
            }
            for (Variable variable : binaryVariables) {
                writer.write(" " + variable.getName());
            }
            if (!binaryVariables.isEmpty()) {
                writer.write("\n");
            }
            if (!integerVariables.isEmpty()) {
                writer.write("Generals\n");
            }
            for (Variable variable : integerVariables) {
                writer.write(" " + variable.getName());
            }
            if (!integerVariables.isEmpty()) {
                writer.write("\n");
            }
            writer.write("End\n");
            writer.close();
        } catch (IOException e) {
            throw new OutputFileException(filename);
        }
    }

    private static String formatVariable(Variable variable, double coefficient) {
        String result = "";
        if (coefficient == 0) {
            return result;
        }
        if (coefficient < 0) {
            result += "- ";
        }
        else {
            result += "+ ";
        }
        result += Math.abs(coefficient) + " " + variable.getName();
        return result;
    }
}
