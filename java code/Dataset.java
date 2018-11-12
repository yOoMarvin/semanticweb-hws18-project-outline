

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

public class Dataset {
	private ArrayList<Instance> dataset;

	public Dataset() {
		dataset = this.runSparql();
	}

	ArrayList<Instance> getDataset() {
		return this.dataset;
	}

	private ArrayList<Instance> runSparql() {
		String service = "http://linkedgeodata.org/sparql"; // link to the sparql endpoint of linkedgeodata
		String sparqlQuery = "Prefix lgdo: <http://linkedgeodata.org/ontology/> Prefix geom: <http://geovocab.org/geometry#> Prefix ogc:<http://www.opengis.net/ont/geosparql#> Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> Prefix bif:<bif:> Select ?s ?l ?g ?type From <http://linkedgeodata.org> {?s a ?type ; rdfs:label ?l ; geom:geometry [ogc:asWKT ?g] .Filter(bif:st_intersects (?g, bif:st_point (8.476682, 49.483752), 0.05)) .}";
		QueryExecution qexec = QueryExecutionFactory.sparqlService(service, sparqlQuery);
		ResultSet results = qexec.execSelect();

		ArrayList<Instance> instances = new ArrayList<Instance>();
		String type;
		String rememberType = "defaultCategoryName";
		String name = "";
		ArrayList<String> categories = new ArrayList<String>();
		String coordinates = "";
		boolean firstnode = true;
		while (results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			RDFNode node1 = sol.get("?s");
			RDFNode node2 = sol.get("?l");
			RDFNode node3 = sol.get("?g");
			RDFNode node4 = sol.get("?type");
			type = node1.toString();
			if (type.equals(rememberType)) {
				categories.add(node4.toString());
			} else {
				if (firstnode == true) {
					firstnode = false;
				} else {
					Instance instance = this.createInstance(name, coordinates, categories);
				}
				rememberType = type;
				name = node2.toString();
				coordinates = node3.toString();
				categories.add(node4.toString());
				System.out.println(type + "   " + name + "   " + categories + "   " + coordinates);
			}
		}
		return instances;
	}

	private Instance createInstance(String name, String coordinates, ArrayList<String> categories) {
		Instance inst = new Instance();
		inst.setName(name);
		String[] longLat = this.getLongitudeAndLatitude(coordinates);
		inst.setLongitude(longLat[0]);
		inst.setLatitude(longLat[1]);
		// get all categories
		ArrayList<Category> cats = new ArrayList<Category>();
		for (int i = 0; i < categories.size(); i++) {
			String categoryName = categories.get(i).toString();
			Category cat = new Category(categoryName, 1.0);
			cats.add(cat);
		}
		inst.setInstitutionClasses(cats);
		return inst;
	}

	private String[] getLongitudeAndLatitude(String coordinates) {
		Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(coordinates);
		while (m.find()) {
			coordinates = m.group(1);
		}
		String[] splitCoordinates = coordinates.split(",");
		ArrayList<String> longitudes = new ArrayList<String>();
		ArrayList<String> latitudes = new ArrayList<String>();
		for (int i = 0; i < splitCoordinates.length; i++) {
			String[] oneCoordinate = splitCoordinates[i].split(" ");
			longitudes.add(oneCoordinate[0]);
			latitudes.add(oneCoordinate[1]);
		}

		double meanLongitude = 0.0;
		double meanLatitude = 0.0;

		for (int i = 0; i < longitudes.size(); i++) {
			meanLongitude = meanLongitude + Double.parseDouble(longitudes.get(i));
			meanLatitude = meanLatitude + Double.parseDouble(latitudes.get(i));
		}

		meanLongitude = meanLongitude / longitudes.size();
		meanLatitude = meanLatitude / latitudes.size();

		String sMeanLongitude = Double.toString(meanLongitude);
		String sMeanLatitude = Double.toString(meanLatitude);

		String[] returnArray = new String[] { sMeanLongitude, sMeanLatitude };

		return returnArray;
	}
}