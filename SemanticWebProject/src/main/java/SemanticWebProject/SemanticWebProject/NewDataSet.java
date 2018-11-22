package SemanticWebProject.SemanticWebProject;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;

public class NewDataSet {
	private ArrayList<Instance> dataset;
	private double longitude;
	private double latitude;

	public NewDataSet(double longitude, double latitude) {
		// call with Dataset data = new Dataset("8.476682", "49.483752"); to have the Wasserturm as center
		this.longitude = longitude;
		this.latitude = latitude;
		// dataset = this.runSparql();
		dataset = this.runSparqlDbpedia();
	}

	public ArrayList<Instance> getDataset() {
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
		qexec.setTimeout(20, TimeUnit.SECONDS);
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
					categories = new ArrayList<String>();
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
		inst.setLongitude(Double.parseDouble(longLat[0]));
		inst.setLatitude(Double.parseDouble(longLat[1]));
		ArrayList<Category> cats = new ArrayList<Category>();
		for (int i = 0; i < categories.size(); i++) {
			String categoryName = categories.get(i).toString();
			Double categoryWeight = 0.0;
			if (categoryName.contains("HistoricThing") || categoryName.contains("TourismThing") || categoryName.contains("Shop")
					|| categoryName.contains("EmergencyThing") || categoryName.contains("SportThing") || categoryName.contains("School")
					|| categoryName.contains("Office") || categoryName.contains("Leisure") || categoryName.contains("ManMadeThing")) {
				categoryWeight = 1.5;
			} else if (categoryName.contains("Amenity")) {
				categoryWeight = 1.0;
			} else if (categoryName.contains("RailwayThing")) {
				categoryWeight = 0.1;
			} else if (categoryName.contains("Restaurant") || categoryName.contains("University") || categoryName.contains("Museum")
					|| categoryName.contains("Bar") || categoryName.contains("Bakery") || categoryName.contains("Cinema")
					|| categoryName.contains("Theater") || categoryName.contains("Hospital") || categoryName.contains("Church")) {
				categoryWeight = 2.0;
			}
			Category cat = new Category(categoryName, categoryWeight);
			cats.add(cat);
		}

		inst.setInstitutionClasses(cats);
		return inst;
	}

	private Instance createInstanceDbpedia(String name, String[] coordinates, ArrayList<String> categories) {
		Instance inst = new Instance();
		inst.setName(name);
		Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(coordinates[0]);
		while (m.find()) {
			coordinates[0] = m.group(1);
		}
		String[] splitCoord = coordinates[0].split("\\^\\^");
		inst.setLongitude(Double.parseDouble(splitCoord[0]));
		m = Pattern.compile("\\(([^)]+)\\)").matcher(coordinates[1]);
		while (m.find()) {
			coordinates[1] = m.group(1);
		}
		splitCoord = coordinates[1].split("\\^\\^");
		inst.setLatitude(Double.parseDouble(splitCoord[0]));
		ArrayList<Category> cats = new ArrayList<Category>();
		for (int i = 0; i < categories.size(); i++) {
			String categoryName = categories.get(i).toString();
			Double categoryWeight = 0.0;
			if (categoryName.contains("Company") || categoryName.contains("SportFacility") || categoryName.contains("Organization")
					|| categoryName.contains("EducationalInstitution") || categoryName.contains("ArchitecturalStructure")
					|| categoryName.contains("School")) {
				categoryWeight = 1.5;
			} else if (categoryName.contains("Building")) {
				categoryWeight = 1.0;
			} else if (categoryName.contains("RailwayThing")) {
				categoryWeight = 0.1;
			} else if (categoryName.contains("Restaurant") || categoryName.contains("University") || categoryName.contains("Museum")
					|| categoryName.contains("Airport") || categoryName.contains("Stadium")) {
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

	private ArrayList<Instance> runSparqlDbpedia() {
		String service = "http://dbpedia.org/sparql";
		String query = "Prefix geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> Prefix dbo:<http://dbpedia.org/ontology/>" +
				"Prefix rdfs:<http://www.w3.org/2000/01/rdf-schema#> Prefix bif:<bif:>" +
				"SELECT distinct ?cat ?type ?name ?long ?lat { ?cat a ?type; rdfs:label ?name; geo:long ?long; geo:lat ?lat. FILTER (bif:st_intersects( bif:st_point (?long, ?lat), bif:st_point ("
				+ Double.toString(this.longitude)
				+ ","
				+ Double.toString(this.latitude)
				+ "), 10)). FILTER (?type = dbo:Building || ?type = dbo:Museum || ?type = dbo:SportFacility || ?type = dbo:Stadium || ?type = dbo:Organization || ?type = dbo:Company || ?type = dbo:University || ?type = dbo:EducationalInstitution || ?type = dbo:Airport|| ?type = dbo:ArchitecturalStructure || ?type = dbo:Restaurant || ?type = dbo:School ). FILTER (lang(?name) = 'en').}";
		Query q = QueryFactory.create(query);
		QueryExecution qexec = QueryExecutionFactory.sparqlService(service, query);
		ResultSet results = qexec.execSelect();

		ArrayList<Instance> instances = new ArrayList<Instance>();
		String type;
		String rememberType = "defaultCategoryName";
		String name = "";
		ArrayList<String> categories = new ArrayList<String>();
		String[] coordinates = { "long", "lat" };
		boolean firstnode = true;
		while (results.hasNext()) {
			QuerySolution sol = results.nextSolution();
			RDFNode node1 = sol.get("?cat");
			RDFNode node2 = sol.get("?name");
			RDFNode node3 = sol.get("?lat");
			RDFNode node4 = sol.get("?long");
			RDFNode node5 = sol.get("?type");
			type = node1.toString();
			if (type.equals(rememberType)) {
				categories.add(node5.toString());
			} else {
				if (firstnode == true) {
					firstnode = false;
				} else {
					Instance instance = this.createInstanceDbpedia(name, coordinates, categories);
					instances.add(instance);
					categories.clear();
				}
				rememberType = type;
				name = node2.toString();
				String[] nameParts = name.split("\"", 1);
				name = nameParts[0];
				nameParts = name.split("@");
				name = nameParts[0];
				coordinates[0] = node4.toString();
				coordinates[1] = node3.toString();
				categories.add(node5.toString());
			}
		}
		return instances;
	}
}
