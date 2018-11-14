package SemanticWebProject.SemanticWebProject;

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
	private String longitude;
	private String latitude;

	public Dataset(String longitude, String latitude) {
		// call with Dataset data = new Dataset("8.476682", "49.483752"); to have the Wasserturm as center
		this.longitude = longitude;
		this.latitude = latitude;
		dataset = this.runSparql();
	}

	ArrayList<Instance> getDataset() {
		return this.dataset;
	}

	private ArrayList<Instance> runSparql() {

		String service = "http://linkedgeodata.org/sparql";
		String sparqlQuery = "Prefix lgdo: <http://linkedgeodata.org/ontology/> Prefix geom: <http://geovocab.org/geometry#> Prefix ogc:<http://www.opengis.net/ont/geosparql#> Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> Prefix bif:<bif:> Select ?s ?l ?g ?type From <http://linkedgeodata.org> {?s a ?type ; rdfs:label ?l ; geom:geometry [ogc:asWKT ?g] "
				+ ".Filter(bif:st_intersects (?g, bif:st_point ("
				+ this.longitude + "," + this.latitude + "), 0.05)) "
				+ ".Filter(?type = lgdo:Amentiy || ?type = lgdo:HistoricThing || ?type = lgdo:TourismThing || ?type = lgdo:EmergencyThing || ?type = lgdo:SportThing || ?type = lgdo:Shop || ?type = lgdo:Office || ?type = lgdo:ManMadeThing || ?type = lgdo:Leisure || ?type = lgdo:RailwayThing "
				+ "||?type = lgdo:Restaurant || ?type = lgdo:University || ?type = lgdo:Museum || ?type = lgdo:School || ?type = lgdo:Bar || ?type = lgdo:Cinema || ?type = lgdo:Theater || ?type = lgdo:Bakery || ?type = lgdo:Hospital || ?type = lgdo:Church ) .}";
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
					// System.out.println(instance.toString());
					instances.add(instance);
				}
				rememberType = type;
				name = node2.toString();
				coordinates = node3.toString();
				categories.add(node4.toString());
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
		ArrayList<Category> cats = new ArrayList<Category>();
		for (int i = 0; i < categories.size(); i++) {
			String categoryName = categories.get(i).toString();
			Double categoryWeight = 0.0;
			if (categoryName.contains("HistoricThing") || categoryName.contains("TourismThing") || categoryName.contains("Shop")
					|| categoryName.contains("EmergencyThing") || categoryName.contains("SportThing") || categoryName.contains("RailwayThing")
					|| categoryName.contains("Office") || categoryName.contains("Leisure") || categoryName.contains("ManMadeThing")) {
				categoryWeight = 1.5;
			} else if (categoryName.contains("Amenity")) {
				categoryWeight = 1.0;
			} else if (categoryName.contains("Restaurant") || categoryName.contains("University") || categoryName.contains("Museum")
					|| categoryName.contains("School") || categoryName.contains("Bar") || categoryName.contains("Bakery") || categoryName.contains("Cinema")
					|| categoryName.contains("Theater") || categoryName.contains("Hospital") || categoryName.contains("Church")) {
				categoryWeight = 2.0;
			}
			Category cat = new Category(categoryName, categoryWeight);
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