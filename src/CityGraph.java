import geojson.Feature;
import geojson.IFeatureCollection;
import geojson.IProjector;
import geojson.LatLon;
import pathfinder.Graph;
import pathfinder.GraphEdge;
import pathfinder.GraphNode;
import processing.core.PVector;
import utils.GeometryUtils;

import java.util.ArrayList;

/**
 * Created at 14/07/16
 *
 * @author tmshv
 */
public class CityGraph {
    ArrayList<Crossroad> crossroads;
    ArrayList<Road> roads;
    Graph graph;

    int strokeColor = 0xffffffff;
    int strokeThickness = 1;

    private LatLon[] bound;

    PVector topLeft;
    PVector rightBottom;
    private String name;

    public CityGraph() {
        rightBottom = new PVector();
        topLeft = new PVector();
        roads = new ArrayList<>();
        crossroads = new ArrayList<>();
        graph = new Graph();
    }

    public void loadFeatures(IFeatureCollection fc, IProjector projector) {
        ArrayList<Feature> features = fc.getFeatures();

        bound = fc.bounds();
        topLeft = projector.project(bound[0]);
        rightBottom = projector.project(bound[1]);

        GraphEdge edge;
        for (Feature f : features) {
            Path path = new Path();
            f.geometry.coords
                    .stream()
                    .map(projector::project)
                    .forEach(path::add);

            PVector first = projector.project(f.geometry.first());
            PVector last = projector.project(f.geometry.last());

            Crossroad cr1 = getCrossroadAt(first);
            Crossroad cr2 = getCrossroadAt(last);
            float weight = cr1.coord.dist(cr2.coord);

            edge = addGraphEdge(cr1.node.id(), cr2.node.id(), weight);
            addRoad(new Road(path.coords, cr1, cr2, edge));

            edge = addGraphEdge(cr2.node.id(), cr1.node.id(), weight);
            addRoad(new Road(path.reverse().coords, cr2, cr1, edge));
        }
    }

    public void addRoad(Road road) {
        roads.add(road);
    }

    public Road getRoad(GraphEdge edge) {
        for (Road r : roads) {
            if (r.cr1.node.id() == edge.from().id() && r.cr2.node.id() == edge.to().id()) return r;
        }
        return null;
    }

    public PVector getGraphNodeCoord(GraphNode node) {
        Crossroad cr = crossroads.get(node.id());
        return cr.coord;
    }

    private GraphNode addGraphNode(int id, float x, float y) {
        GraphNode node = new GraphNode(id, x, y);
        graph.addNode(node);
        return node;
    }

    private GraphEdge addGraphEdge(int from, int to, float weight) {
        graph.addEdge(from, to, weight);
        return graph.getEdge(from, to);
    }

    public Crossroad getCrossroadAt(PVector ll) {
        for (Crossroad cr : crossroads) {
            if (cr.coordIsEqual(ll)) {
                return cr;
            }
        }

        int id = crossroads.size();
        GraphNode n = addGraphNode(id, ll.x, ll.y);
        Crossroad cr = new Crossroad(ll, n);
        crossroads.add(cr);
        return cr;
    }

    public int getCrossroadIndex(PVector ll) {
        int length = this.crossroads.size();
        for (int i = 0; i < length; i++) {
            if (this.crossroads.get(i).coordIsEqual(ll)) return i;
        }
        return -1;
    }

    public int getCrossroadIndex(Crossroad cr) {
        return getCrossroadIndex(cr.coord);
    }

    // void drawCrossroads(){
    //   pushStyle();

    //   for (Crossroad cr : this.crossroads) {
    //     noStroke();
    //     fill(255, 30);

    //     float s = map(cr.roads.mass(), 0, 10, 10, 30);
    //     PVector p = projector.project(cr.coord);
    //     ellipse(p.x, p.y, s, s);
    //   }
    //   popStyle();
    // }

    public LatLon getCenter() {
        return new LatLon(
                (bound[0].lat + bound[1].lat) / 2,
                (bound[0].lon + bound[1].lon) / 2
        );
    }

    public Crossroad findNearestCrossroadTo(PVector v) {
        float max_dist = 6378137; //<>//

        Crossroad choise = null;
        for (Crossroad cr : this.crossroads) {
            float dist = cr.coord.dist(v);
            if (dist < max_dist) {
                max_dist = dist;
                choise = cr;
            }
        }
        return choise;
    }

    public GraphEdge findNearestGraphEdgeTo(PVector v) {
        GraphEdge nearest = null;
        float minDist = Float.MAX_VALUE;

        GraphEdge[] edges = graph.getAllEdgeArray();
        for (GraphEdge ge : edges) {
            PVector p1 = getGraphNodeCoord(ge.from());
            PVector p2 = getGraphNodeCoord(ge.to());

            PVector projectionVertex = GeometryUtils.projectVertexOnLine(v, p1, p2);
            String pointClass = GeometryUtils.classify(projectionVertex, p1, p2);

            if (pointClass != "between") continue;

            float d = PVector.dist(v, projectionVertex);
            if (d < minDist) {
                minDist = d;
                nearest = ge;
            }
        }

        return nearest;
    }

    public Road findNearestRoadTo(PVector v) {
        GraphEdge nearest = findNearestGraphEdgeTo(v);
        if (nearest != null) {
            for (Road r : roads) {
                if (r.edge == nearest) return r;
            }
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
