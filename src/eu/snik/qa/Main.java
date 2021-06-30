package eu.snik.qa;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.simmetrics.StringMetric;
import org.simmetrics.builders.StringMetricBuilder;
import org.simmetrics.metrics.DamerauLevenshtein;
import org.simmetrics.simplifiers.Simplifiers;

public class Main {

	static final String[] templates = {"What is <Class>?","What are <Class>s?",
	"Who <RolePredicate> <RoleObject>?", "Who is <Class>?","What <Predicate> <Object>"};

	static record Triple(String subject,String predicate, String object) {}
	
	public static void main(String[] args) throws IOException {
		Map<String,String> definitions = new HashMap<String,String>();
		try(Reader in = new FileReader("definitions.csv"))
		{
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
			for(var record: records)
			{
				definitions.put(record.get(0),record.get(1));
			}
		}
		
		Set<Triple> roleTriples = new HashSet<Triple>();
		Set<String> roles = roleTriples.stream().map(t->t.subject).collect(Collectors.toSet());
		try(Reader in = new FileReader("roletriples.csv"))
		{
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
			for(var record: records)
			{
				roleTriples.add(new Triple(record.get(0),record.get(1),record.get(2)));
			}
		}

		Set<Triple> triples = new HashSet<Triple>();
		Set<String> subjects = roleTriples.stream().map(t->t.subject).collect(Collectors.toSet());
		try(Reader in = new FileReader("triples.csv"))
		{
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(in);
			for(var record: records)
			{
				triples.add(new Triple(record.get(0),record.get(1),record.get(2)));
			}
		}
		
		Map<String,String> answers = new HashMap<>(); 
		for(final String template: templates)
		{
			if(template.contains("<Class>"))
			{
				for(String clazz: definitions.keySet())
				{
					answers.put(template.replace("<Class>", clazz), definitions.get(clazz));
				}
			}

			if(template.contains("<RolePredicate>")&&template.contains("<RoleObject>"))
			{
				for(Triple triple: roleTriples)
				{
					answers.put(template.replace("<RolePredicate>", triple.predicate).replace("<RoleObject>", triple.object), triple.subject);
				}
			}

			if(template.contains("<Predicate>")&&template.contains("<Object>"))
			{
				for(Triple triple: triples)
				{
					answers.put(template.replace("<Predicate>", triple.predicate).replace("<Object>", triple.object), triple.subject);
				}
			}

		}

		
		try(Writer out = new FileWriter("answers.json"))
		{			
			JSONArray json = new JSONArray();
			for(var entry: answers.entrySet())
			{
				JSONObject obj = new JSONObject();
				obj.put("question",entry.getKey());
				obj.put("answer",entry.getValue());
				json.put(obj);
			}
			out.write(json.toString());
		}
		//answers.forEach((a,b) -> {System.out.println(a+"  Answer: "+b);});
		
		
		Scanner input = new Scanner(System.in);
		StringMetric metric = StringMetricBuilder.with(new DamerauLevenshtein())
				.simplify(Simplifiers.toLowerCase(Locale.ENGLISH))
				.simplify(Simplifiers.replaceNonWord())
				.build();


		while(true)
		{
			double max = 0;
			String line = input.nextLine().replace(" a "," ").replace(" the ", " ").replace(" typical ", " ");
			
			String best = null;
			for(String question: answers.keySet())
			{
				double score = metric.compare(question,line);
				if(score>max)
				{
					//System.out.println(score+": "+question);
					max=score;
					best = question;
				}
			}

			System.out.println("Answer: "+answers.get(best)+" "+max);
			
		}
		
		
	}

}
