package net.lintim.generator;

import net.lintim.exception.DataInconsistentException;
import net.lintim.model.Line;
import net.lintim.model.LineCollection;
import net.lintim.model.Link;
import net.lintim.model.PublicTransportationNetwork;

import java.util.Collection;
import java.util.Vector;

/**
 * Derives a line pool from a line concept by splitting lines.
 */
public class LinePoolFromLineConceptGenerator {
    PublicTransportationNetwork ptn;
    LineCollection lc;

    Integer minimalLength = 1;

    public enum LinePoolModel {
        SEGMENTS, SEGMENTS_BETWEEN_TURNS
    }

    LinePoolModel model;

    /**
     * Constructor.
     *
     * @param lc The reference line concept.
     * @param minimalLength The minimal line length.
     * @param model The line pool model.
     */
    public LinePoolFromLineConceptGenerator(LineCollection lc,
            Integer minimalLength, LinePoolModel model) {

        this.ptn = lc.getPublicTransportationNetwork();
        this.lc = lc;
        this.minimalLength = minimalLength;
        this.model = model;

    }

    /**
     * Adds lines to <code>pool</code> by splitting those from the reference
     * line concept.
     *
     * @param pool The line pool.
     * @throws DataInconsistentException
     */
    public void linePoolFromLineConcept(LineCollection pool) throws
    DataInconsistentException{
        if(pool == null){
            throw new DataInconsistentException("pool is null");
        }

        Collection<Line> toIterate;

        if(lc.isUndirected()){
            toIterate = lc.getUndirectedIndexLineMap().values();
        }
        else{
            toIterate = lc.getDirectedLines();
        }

        // This way we will stay consistent with original numbering.
        for(Line line : toIterate){
            pool.addLine(line);
        }

        for(Line line : toIterate){
            Boolean undirected = line.isUndirected();

            Integer size = line.getLinks().size();
            Vector<Link> links = new Vector<Link>(line.getLinks());
            Integer effectiveMinimalSize = Math.min(size, minimalLength);

            for(Integer i=0; i < size-effectiveMinimalSize+1; i++){
                if(model == LinePoolModel.SEGMENTS_BETWEEN_TURNS &&
                        !links.get(i).getFromStation().getVehicleCanTurn()){
                    continue;
                }
                for(Integer j=effectiveMinimalSize; j < size-i+1; j++){
                    if(model == LinePoolModel.SEGMENTS_BETWEEN_TURNS &&
                            !links.get(i+j-1).getToStation().getVehicleCanTurn()){
                        continue;
                    }

                    // Do not add the original lines. We have already added them
                    // before.
                    if(i==0 && j == size){
                        continue;
                    }

                    // FIXME use proper interface
                    Integer smallestFreeIndex = pool.getSmallestFreeLineIndex();
                    Line toAdd = new Line(undirected, smallestFreeIndex);

                    for(Integer k=0; k < j; k++){
                        toAdd.addLinkEnd(links.get(i+k));
                    }

                    pool.addLine(toAdd);
                    toAdd.setFrequency(0);

                }
            }


        }

    }

}
