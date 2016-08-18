
package org.exist.xquery.modules.geom;

import org.apache.log4j.Logger;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.Dependency;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.Profiler;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.DoubleValue;
import org.exist.xquery.value.BooleanValue;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.EmptySequence;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.NumericValue;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.operation.predicate.RectangleContains;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;

/**
 * @author William Summers
 */
public class GeomFunctions extends BasicFunction {

	private static final Logger logger = Logger.getLogger(GeomFunctions.class);
	private WKTReader wktReader = new WKTReader();
	private Geometry geom1, geom2;

    public final static FunctionSignature signature[] = {
        new FunctionSignature(
                new QName("contains", GeomModule.NAMESPACE_URI),
                "Every point of the second geometry is a point of the first geometry, and the interiors of the two geometries have at least one point in common.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry1", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in WKT format."),
                    new FunctionParameterSequenceType("geometry2", Type.STRING, Cardinality.EXACTLY_ONE, "Another geometry in WKT format.")
                },
                new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "the result")
                ),
        new FunctionSignature(
                new QName("intersects", GeomModule.NAMESPACE_URI),
                "The two geometries have at least one point in common",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry1", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in WKT format."),
                    new FunctionParameterSequenceType("geometry2", Type.STRING, Cardinality.EXACTLY_ONE, "Another geometry in WKT format.")
                },
                new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "the result")
                ),
        new FunctionSignature(
                new QName("getArea", GeomModule.NAMESPACE_URI),
                "Returns the area of this geometry.",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in WKT format."),
                },
                new FunctionReturnSequenceType(Type.STRING, Cardinality.EXACTLY_ONE, "the result")
                ),
        new FunctionSignature(
                new QName("getCentroid", GeomModule.NAMESPACE_URI),
                "Computes the centroid of this geometry",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in WKT format."),
                },
                new FunctionReturnSequenceType(Type.STRING, Cardinality.EXACTLY_ONE, "the result")
                ),
        new FunctionSignature(
                new QName("isEmpty", GeomModule.NAMESPACE_URI),
                "The geometry is empty",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry1", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in WKT format."),
                },
                new FunctionReturnSequenceType(Type.BOOLEAN, Cardinality.EXACTLY_ONE, "the result")
                ),
        new FunctionSignature(
                new QName("bboxToWKT", GeomModule.NAMESPACE_URI),
                "Convert coordintates in minX, minY, maxX, maxY to WKT",
                new SequenceType[] {
                    new FunctionParameterSequenceType("geometry", Type.STRING, Cardinality.EXACTLY_ONE, "A geometry in minX, minY, maxX, maxY format."),
                },
                new FunctionReturnSequenceType(Type.STRING, Cardinality.EXACTLY_ONE, "the result")
                )

    };

    /**
     * @param context
     */
    public GeomFunctions(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    /* (non-Javadoc)
     * @see org.exist.xquery.Expression#eval(org.exist.dom.DocumentSet, org.exist.xquery.value.Sequence, org.exist.xquery.value.Item)
     */
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        if (context.getProfiler().isEnabled()) {
            context.getProfiler().start(this);
            context.getProfiler().message(this, Profiler.DEPENDENCIES, "DEPENDENCIES", Dependency.getDependenciesName(this.getDependencies()));
            if (contextSequence != null){
                context.getProfiler().message(this, Profiler.START_SEQUENCES, "CONTEXT SEQUENCE", contextSequence);
            }
        }

        Sequence result;
        double calcValue=0;
        String functionName = getSignature().getName().getLocalName();

        if("contains".equals(functionName)) {
	        Sequence seqA = args[0].convertTo(Type.STRING);
	        StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
	        Sequence seqB = args[1].convertTo(Type.STRING);
	        StringValue valueB = (StringValue)seqB.itemAt(0).convertTo(Type.STRING);
			try {
				geom1 = wktReader.read(valueA.toString());
				geom2 = wktReader.read(valueB.toString());
				result = new BooleanValue(geom1.contains(geom2));
			} catch (ParseException pe) {
				result = new BooleanValue(false);
			}

        } else if("intersects".equals(functionName)) {
	        Sequence seqA = args[0].convertTo(Type.STRING);
	        StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
	        Sequence seqB = args[1].convertTo(Type.STRING);
	        StringValue valueB = (StringValue)seqB.itemAt(0).convertTo(Type.STRING);
			try {
				geom1 = wktReader.read(valueA.toString());
				geom2 = wktReader.read(valueB.toString());
				result = new BooleanValue(geom1.intersects(geom2));
			} catch (ParseException pe) {
				result = new EmptySequence();
			}

        } else if("getArea".equals(functionName)) {
	        Sequence seqA = args[0].convertTo(Type.STRING);
	        StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
			try {
				geom1 = wktReader.read(valueA.toString());
				result = new StringValue(Double.toString(geom1.getArea()));
			} catch (ParseException pe) {
				result = new EmptySequence();
			}

        } else if("getCentroid".equals(functionName)) {
	        Sequence seqA = args[0].convertTo(Type.STRING);
	        StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
			try {
				geom1 = wktReader.read(valueA.toString());
				Point p = geom1.getCentroid();
				result = new StringValue(p.getX() + " " + p.getY());
			} catch (ParseException pe) {
				result = new EmptySequence();
			}

        } else if("isEmpty".equals(functionName)) {
			Sequence seqA = args[0].convertTo(Type.STRING);
			StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
			try {
				geom1 = wktReader.read(valueA.toString());
				result = new BooleanValue(geom1.isEmpty());
			} catch (ParseException pe) {
				result = new EmptySequence();
			}

        } else if("bboxToWKT".equals(functionName)) {
	        Sequence seqA = args[0].convertTo(Type.STRING);
	        StringValue valueA = (StringValue)seqA.itemAt(0).convertTo(Type.STRING);
	        String bbox = valueA.getStringValue();
			try {
				//parse the minX, minY, maxX, maxY
				// coords = (minx, miny), (maxx, miny), (maxx, maxy), (minx, maxy), (minx, miny)
				// POLYGON ((0 1,2 1,2 3,0 3,0 1))
				String delim = ",";
				String tokens[] = bbox.split(delim);
				result = new StringValue("POLYGON(("+tokens[0] +" "+ tokens[1] + "," + tokens[2] + " " + tokens[1]+ "," + tokens[2] + " " + tokens[3]+ "," + tokens[0] + " " + tokens[3] + "," + tokens[0] + " " + tokens[1] + "))");
			} catch (Exception e) {
				result = new EmptySequence();
			}


        } else {
            throw new XPathException(this, "Function "+functionName+" not found.");
        }

        if (context.getProfiler().isEnabled()){
            context.getProfiler().end(this, "", result);
        }

        return result;
    }

}
