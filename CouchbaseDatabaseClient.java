

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Scanner;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.couchbase.client.core.retry.FailFastRetryStrategy;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonArray;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.query.N1qlQuery;

public class CouchbaseDatabaseClient {

	private static CouchbaseEnvironment env;
	private static Cluster cbCluster;
	private static Bucket bucket;

	private static void init() {
		env = DefaultCouchbaseEnvironment.builder().connectTimeout(10000).retryStrategy(FailFastRetryStrategy.INSTANCE)
				.build();
		cbCluster = CouchbaseCluster.create(env, Arrays.asList("127.0.0.1"));
		cbCluster.authenticate("admin", "Sdxio354@");
		bucket = cbCluster.openBucket("khalid");
	}

	private static void shutdown() {
		bucket.close();
		cbCluster.disconnect();
	}

	private static JsonDocument createDocument(String docId,String emailId,String userName,String Password) {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String dateTime=now.toString();
		JsonObject user = JsonObject.create().put("id", docId).put("emailId",emailId).put("Name", userName)
				.put("passcode",Password).put("dateTime",dateTime).put("LastLoginTime",dateTime);
		JsonDocument document = JsonDocument.create(docId, user);
		return document;
	}
    private static void login(String email){
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		LocalDateTime now = LocalDateTime.now();
		String dateTime=now.toString();
		JsonObject pVal = JsonObject.create().put("emailId", email).put("LastLoginTime",dateTime);
		String query = " UPDATE `khalid`"+" SET LastLoginTime = $LastLoginTime" +" WHERE emailId =$emailId";
		bucket.query(N1qlQuery.parameterized(query, pVal)).forEach(row -> {
		});
	}

	private static boolean validateEmailId(String emailId){
		String regex = "^[\\w!#$%&'*+/=?`{|}~^-]+(?:\\.[\\w!#$%&'*+/=?`{|}~^-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6}$";
		Pattern pattern = Pattern.compile(regex);

		Matcher matcher = pattern.matcher(emailId);
		return matcher.matches();
	}
	private static boolean ValidatePassword(String password)
	{
		boolean valid = true;
		if (password.length() > 15 || password.length() < 8)
		{
			System.out.println("Password should be less than 15 and more than 8 characters in length.");
			valid = false;
		}
		String upperCaseChars = "(.*[A-Z].*)";
		if (!password.matches(upperCaseChars ))
		{
			System.out.println("Password should contain atleast one upper case alphabet");
			valid = false;
		}
		String lowerCaseChars = "(.*[a-z].*)";
		if (!password.matches(lowerCaseChars ))
		{
			System.out.println("Password should contain atleast one lower case alphabet");
			valid = false;
		}
		String numbers = "(.*[0-9].*)";
		if (!password.matches(numbers ))
		{
			System.out.println("Password should contain atleast one number.");
			valid = false;
		}
		String specialChars = "(.*[,~,!,@,#,$,%,^,&,*,(,),-,_,=,+,[,{,],},|,;,:,<,>,/,?].*$)";
		if (!password.matches(specialChars ))
		{
			System.out.println("Password should contain atleast one special character");
			valid = false;
		}
		if (valid)
		{
			return true;
		}
		return false;
	}

	private static JsonArray queryEmail(String email) {
		JsonObject pVal = JsonObject.create().put("emailId", email);
		String query = "SELECT * FROM `khalid` " + " WHERE emailId =$emailId";
		JsonArray output = JsonArray.create();
		bucket.query(N1qlQuery.parameterized(query, pVal)).forEach(row -> {
			output.add(row.value());
		});
		return output;
	}

	private static JsonArray queryPassword(String email, String passcode) {
		JsonObject pVal = JsonObject.create().put("passcode", passcode).put("emailId", email);
		String query = "SELECT * FROM `khalid` " + "WHERE  emailId =$emailId and passcode =$passcode";
		JsonArray output = JsonArray.create();

		bucket.query(N1qlQuery.parameterized(query, pVal)).forEach(row -> {
			output.add(row.value());
		});
		return output;
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		init();
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		boolean temp=true;
		while(temp){
			System.out.println("1.Press 1 to Bulk Upload\n2.Press 2 to Register\n3.Press 3 to Login\n4.Press 4 to EXIT");
			int c=Integer.parseInt(br.readLine());
			switch (c){
				case 1:
					try {
						//----------Writing into file
						boolean temp1 = true;
						StringBuilder sb = new StringBuilder();
						System.out.println("Enter the email-Id space userName space password");
						System.out.println("Write 0 to EXIT");
						while (temp1) {

							String[] input = br.readLine().split(" ");
							if(input.length==1){
                                temp1=false;
                                continue;
							}
							if(input.length!=3){
								System.out.println("Invalid Input Please Try Again");
								temp1=false;
								continue;
							}
							if (!validateEmailId(input[0])) {
								System.out.println("Invalid Email Please try again");
								temp1 = false;
								continue;
							}

							if (!ValidatePassword(input[2])) {
								System.out.println("Invalid Password Please try again");
								temp1 = false;
								continue;
							}
							sb.append(input[0] + " " + input[1] + " " + input[2] + "\n");
						}
						BufferedWriter bw = new BufferedWriter(new FileWriter("DataBase.txt"));
						bw.write(sb.toString());
						bw.close();
						//---------------Reading from file
						Scanner read = new Scanner(new File("DataBase.txt"));
						while(read.hasNextLine()){
							String[] data = read.nextLine().split(" ");
							String docId = UUID.randomUUID().toString();
							bucket.insert(createDocument(docId, data[0], data[1], data[2]));
						}

					}catch (Exception e){
						e.printStackTrace();
					}
					break;

				case 2:
                    boolean em=true;
                    while(em) {
						System.out.println("Enter Email-ID: ");
						String email = br.readLine();
						if (validateEmailId(email)) {
							JsonArray arr = queryEmail(email);
							if(!arr.isEmpty()){
								System.out.println("EmailId already Exist");
							}else {
								System.out.println("Enter UserName: ");
								String userName = br.readLine();
								boolean pass = true;
								while (pass) {
									System.out.println("Enter the Password: ");
									String password = br.readLine();
									if (ValidatePassword(password)) {
										String docId = UUID.randomUUID().toString();
										bucket.insert(createDocument(docId, email, userName, password));
										System.out.println("Successfully Registered ");
										pass = false;
									} else {
										System.out.println("Try Again!");
									}
								}
							}
							em=false;
						}else{
							System.out.println("Invalid Email Please Try Again");
						}
					}
					break;

				case 3:
					System.out.println("Enter your email id: ");
					String email = br.readLine();
					JsonArray arr = queryEmail(email);
					if(arr.isEmpty())
						System.out.println("Email id not present");
					else {
						System.out.println("Email Exist");
						System.out.println("Enter your password:");
						String password = br.readLine();
						JsonArray arr2 = queryPassword(email, password);
						if (arr2.isEmpty())
							System.out.println("Wrong Password! Try Again");
						else {
							System.out.println("Logged In Successfully!");
							System.out.println("Here is your Detail:");
							System.out.println(arr2.get(0) + "\n");
							login(email);
						}
					}
					break;

				case 4:
					temp=false;
					System.out.println("Thankyou!");
					break;
				default:
					System.out.println("Please press right option available!");
			}
		}
		shutdown();
	}
}
