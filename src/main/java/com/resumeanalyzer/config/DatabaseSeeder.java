package com.resumeanalyzer.config;

import com.resumeanalyzer.model.Job;
import com.resumeanalyzer.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initDatabase(JobRepository jobRepository) {
        return args -> {
            if (jobRepository.count() < 100) {
                System.out.println("Seeding 100+ high-quality CSE/IT entry-level/internship jobs...");
                jobRepository.deleteAll();
                
                // Roles list (53 roles)
                String[] jobRoles = {
                    "Java Developer", "Spring Boot Developer", "Backend Developer", "Frontend Developer", 
                    "Full Stack Developer", "React Developer", "Angular Developer", "Node.js Developer", 
                    "MERN Stack Developer", "Python Developer", "Django Developer", "Flask Developer", 
                    "Data Analyst", "Business Analyst", "Data Scientist", "AI Engineer", 
                    "Machine Learning Engineer", "Deep Learning Engineer", "Cloud Engineer", "AWS Engineer", 
                    "Azure Engineer", "DevOps Engineer", "Site Reliability Engineer", "Cyber Security Analyst", 
                    "Security Engineer", "Network Engineer", "QA Engineer", "Automation Tester", 
                    "Manual Tester", "Software Engineer", "Software Developer", "Android Developer", 
                    "Flutter Developer", "Kotlin Developer", "C++ Developer", "Embedded Engineer", 
                    "IoT Engineer", "SQL Developer", "Database Administrator", "UI/UX Designer", 
                    "Product Engineer", "Technical Support Engineer", "System Engineer", "Graduate Engineer Trainee", 
                    "Associate Software Engineer", "Associate Consultant", "IT Support Engineer", "SAP Associate", 
                    "Salesforce Developer", "Oracle Developer", "Blockchain Developer", "Game Developer", 
                    "AR/VR Developer"
                };

                // Category index mapping for each role (0 to 7)
                int[] roleCategories = {
                    1, 1, 1, 0, // Java, Spring Boot, Backend, Frontend
                    2, 0, 0, 1, // Full Stack, React, Angular, Node
                    2, 1, 1, 1, // MERN, Python, Django, Flask
                    3, 3, 4, 4, // Data Analyst, Business Analyst, Data Scientist, AI
                    4, 4, 5, 5, // ML, Deep Learning, Cloud, AWS
                    5, 5, 5, 5, // Azure, DevOps, SRE, Cyber Security
                    5, 5, 3, 3, // Security, Network, QA, Automation Tester
                    3, 2, 2, 6, // Manual Tester, Software Engineer, Software Developer, Android
                    6, 6, 2, 7, // Flutter, Kotlin, C++, Embedded
                    7, 1, 1, 0, // IoT, SQL Developer, DBA, UI/UX Designer
                    2, 3, 3, 2, // Product Engineer, Tech Support, System Engineer, Graduate Trainee
                    2, 2, 3, 7, // Associate SE, Associate Consultant, IT Support, SAP Associate
                    7, 1, 7, 7, // Salesforce, Oracle, Blockchain, Game Dev
                    7 // AR/VR Developer
                };

                // Company Name list (43 companies)
                String[] companyNames = {
                    "Google", "Microsoft", "Amazon", "Apple", "Meta", "Netflix", "Adobe", "Oracle", 
                    "IBM", "Intel", "Cisco", "Dell", "HP", "Accenture", "Capgemini", "TCS", 
                    "Infosys", "Wipro", "Cognizant", "Tech Mahindra", "LTIMindtree", "HCLTech", 
                    "Zoho", "Freshworks", "Paytm", "PhonePe", "Razorpay", "Flipkart", "Swiggy", 
                    "Zomato", "Myntra", "Juspay", "Meesho", "NVIDIA", "AMD", "Qualcomm", 
                    "Bosch", "Siemens", "SAP", "Deloitte", "EY", "KPMG", "PwC"
                };

                String[] companyDescriptions = {
                    "Google is a global technology leader focusing on search, cloud computing, and AI systems.",
                    "Microsoft is a multinational technology corporation producing software, consumer electronics, and cloud services.",
                    "Amazon is a global technology giant focusing on e-commerce, cloud computing (AWS), and AI.",
                    "Apple is a premium technology company designing consumer electronics, software, and services.",
                    "Meta builds technologies that help people connect, find communities, and grow businesses.",
                    "Netflix is a leading entertainment services company offering online streaming of movies and TV series.",
                    "Adobe is a global leader in digital media and digital marketing solutions.",
                    "Oracle is a multinational computer technology corporation specializing in cloud systems and databases.",
                    "IBM is a global technology and consulting corporation specializing in cloud platforms and AI.",
                    "Intel is a world leader in silicon innovation, designing technologies that power devices.",
                    "Cisco is the worldwide leader in networking, security, and collaboration technology.",
                    "Dell is a premier technology provider of hardware, servers, laptops, and IT solutions.",
                    "HP is a leading global provider of personal computing devices, printers, and 3D printing solutions.",
                    "Accenture is a leading global professional services company providing digital, cloud, and security solutions.",
                    "Capgemini is a global leader in partnering with companies to transform and manage their business using tech.",
                    "TCS is a global leader in IT services, digital solutions, and business consulting.",
                    "Infosys is a global leader in next-generation digital services and consulting.",
                    "Wipro is a leading technology services and consulting company focused on cognitive computing.",
                    "Cognizant engineers modern businesses, helping clients modernize technology and transform experiences.",
                    "Tech Mahindra offers innovative and customer-centric information technology experiences globally.",
                    "LTIMindtree is a global technology consulting and digital solutions company enabling enterprises.",
                    "HCLTech is a global technology company home to forward-looking enterprises.",
                    "Zoho is a leading provider of SaaS business software, tools, and office suites.",
                    "Freshworks makes business software that is intuitive, quick to set up, and designed for end users.",
                    "Paytm is a premier digital financial services and mobile payments platform in India.",
                    "PhonePe is a leading digital payments and financial services platform.",
                    "Razorpay is a leading converged payments and financial services platform for businesses.",
                    "Flipkart is India's leading e-commerce marketplace offering a wide range of products.",
                    "Swiggy is a leading on-demand convenience platform offering food and grocery delivery.",
                    "Zomato is a leading technology platform connecting customers, restaurants, and delivery partners.",
                    "Myntra is a premier e-commerce platform for fashion, lifestyle, and beauty products.",
                    "Juspay is a leading payment technology provider specializing in secure, high-volume transactional flows.",
                    "Meesho is a leading social commerce and e-commerce marketplace enabling small retail businesses.",
                    "NVIDIA is the pioneer of GPU-accelerated computing, transforming gaming, research, and AI.",
                    "AMD designs and integrates technology that powers millions of intelligent devices.",
                    "Qualcomm is a world leader in next-generation wireless technologies and mobile processors.",
                    "Bosch is a global provider of technology and services in automotive and industrial goods.",
                    "Siemens is a technology company focused on industry, infrastructure, transport, and healthcare.",
                    "SAP is a global leader in enterprise application software, helping companies run better.",
                    "Deloitte is a leading global provider of audit, consulting, financial advisory, and risk services.",
                    "EY is a global professional services firm specializing in assurance, tax, and transaction advisory.",
                    "KPMG is a global network of professional services firms providing audit, tax, and advisory services.",
                    "PwC is a global professional services network offering audit and assurance services."
                };

                String[] companyColors = {
                    "4285F4", "0078D4", "FF9900", "000000", "0668E1", "E50914", "FF0000", "F80000", 
                    "006699", "0071C5", "00BDF2", "0076C0", "0096D6", "A100FF", "0070AD", "1B365D", 
                    "007CC3", "2C2E83", "0033A0", "E41B13", "F26D21", "00529B", "E02020", "0052CC", 
                    "002E6E", "5F259F", "0C3C9C", "2874F0", "FC8019", "CB202D", "FF3F6C", "000000", 
                    "F43397", "76B900", "ED1C24", "3253DC", "FF0000", "009999", "008FD3", "86BC25", 
                    "FFE600", "00338D", "D04A02"
                };

                // Location list
                String[] locations = {
                    "Bangalore, India", "Hyderabad, India", "Pune, India", "Chennai, India", 
                    "Mumbai, India", "Noida, India", "Gurgaon, India", "Remote", "San Francisco, CA", "Seattle, WA"
                };

                // Remote / Hybrid / On-site
                String[] workModes = {"Remote", "Hybrid", "On-site"};

                // Experience Levels
                String[] experiences = {"0-1 Years", "0-2 Years", "Internship (6 Months)", "Fresh Graduate (0 Years)", "0 Years"};

                // Employment Types
                String[] employmentTypes = {"Full-time", "Internship"};

                List<Job> jobsToSave = new ArrayList<>();

                // Seed exactly 105 jobs
                for (int i = 0; i < 105; i++) {
                    int roleIdx = i % jobRoles.length;
                    int compIdx = i % companyNames.length;
                    int category = roleCategories[roleIdx];
                    
                    String title = jobRoles[roleIdx];
                    
                    // Add "Associate" or "Graduate" or "Intern" to title randomly if it isn't already there
                    if (i % 3 == 0 && !title.contains("Associate") && !title.contains("Graduate") && !title.contains("Trainee")) {
                        title = "Associate " + title;
                    } else if (i % 3 == 1 && !title.contains("Intern")) {
                        title = title + " - Intern";
                    }

                    String company = companyNames[compIdx];
                    String compDesc = companyDescriptions[compIdx];
                    String color = companyColors[compIdx];
                    String location = locations[i % locations.length];
                    String workMode = workModes[i % workModes.length];
                    
                    // Force Location to "Remote" if workMode is "Remote"
                    if ("Remote".equals(workMode)) {
                        location = "Remote";
                    } else if ("Remote".equals(location)) {
                        // If location is remote but workMode is not remote, set location to Bangalore
                        location = "Bangalore, India";
                    }

                    String experience = experiences[i % experiences.length];
                    String employmentType = employmentTypes[i % employmentTypes.length];
                    if (title.contains("Intern")) {
                        experience = "Internship";
                        employmentType = "Internship";
                    }

                    // Salary range
                    String salary;
                    if ("Internship".equals(employmentType)) {
                        String[] stipends = {"25k - 40k per month", "30k - 50k per month", "15k - 25k per month", "40k - 60k per month"};
                        salary = stipends[i % stipends.length];
                    } else {
                        String[] packages = {"6 - 8 LPA", "8 - 12 LPA", "10 - 15 LPA", "12 - 18 LPA", "5 - 7 LPA", "15 - 22 LPA"};
                        salary = packages[i % packages.length];
                    }

                    String logoUrl = "https://ui-avatars.com/api/?name=" + company.replaceAll(" ", "+") + 
                                     "&background=" + color + "&color=fff&size=128&font-size=0.35&bold=true";

                    // Define category-based content
                    String reqSkills = "";
                    String prefSkills = "";
                    String keywords = "";
                    String description = "";
                    String responsibilities = "";
                    String qualifications = "B.E. / B.Tech / BCA / MCA in Computer Science, Information Technology, or related disciplines.";
                    
                    if (category == 0) { // Frontend / UI / UX
                        reqSkills = "HTML5, CSS3, JavaScript, React, Git";
                        prefSkills = "TypeScript, Redux, Tailwind CSS, Responsive Design, Jest";
                        keywords = "HTML, CSS, JavaScript, React, Tailwind, Frontend, UI, UX, Git";
                        description = "We are looking for a passionate Frontend/UI Engineer to help build beautiful, responsive, and highly interactive user interfaces. You will work closely with designers and product managers to bring Figma designs to life.";
                        responsibilities = "- Develop responsive and pixel-perfect web applications using React or Vue.\n- Write clean, maintainable, and efficient front-end code.\n- Collaborate with backend engineers to integrate RESTful APIs.\n- Optimize web pages for maximum speed and scalability.";
                    } else if (category == 1) { // Backend / DB / API
                        reqSkills = "Java, Spring Boot, MySQL, REST API, Git";
                        prefSkills = "Microservices, Hibernate, PostgreSQL, Docker, Redis, Unit Testing";
                        keywords = "Java, Spring Boot, Spring, MySQL, REST, Hibernate, Backend, SQL, Git";
                        description = "Join our engineering team as a Backend Developer, where you will design, build, and maintain robust APIs, database structures, and server-side logic that power our web and mobile applications.";
                        responsibilities = "- Design and build secure, scalable, and high-performance RESTful APIs.\n- Write clean, testable code and maintain schema updates.\n- Optimize database queries and improve caching mechanism.\n- Implement security and data protection protocols.";
                    } else if (category == 2) { // Full Stack / SE / Graduate Trainee
                        reqSkills = "JavaScript, Node.js, React, MongoDB, Express, Git";
                        prefSkills = "TypeScript, AWS, Docker, Next.js, CI/CD, Java, Python";
                        keywords = "JavaScript, Node, React, Express, MongoDB, Full Stack, Software Developer, Git";
                        description = "We are seeking a Full Stack Software Engineer to build and deliver end-to-end features. You will work across both client-side and server-side components, ensuring seamless integration and delivery.";
                        responsibilities = "- Build and deploy robust end-to-end features using the MERN/Java stack.\n- Implement responsive frontend views and secure backend APIs.\n- Write unit tests for front-end and backend components.\n- Participate in design sessions, code reviews, and Agile standups.";
                    } else if (category == 3) { // Data Analyst / QA / Tester / Support
                        reqSkills = "SQL, Excel, Python, JIRA, Manual Testing";
                        prefSkills = "Selenium, Tableau, Power BI, Automation Testing, Linux, scripting";
                        keywords = "SQL, Python, Excel, Tableau, JIRA, QA, Manual Testing, Automation, Selenium";
                        description = "This role is ideal for a junior engineer interested in data analytics or software quality assurance. You will analyze system data, run tests, and assist in identifying application issues.";
                        responsibilities = "- Write and execute manual/automated test cases based on product specifications.\n- Create data dashboards and run SQL queries to retrieve database insights.\n- Document and report bugs in JIRA, and verify bug fixes.\n- Provide technical support and assist in troubleshooting client issues.";
                    } else if (category == 4) { // Data Scientist / AI / ML
                        reqSkills = "Python, Machine Learning, Scikit-Learn, SQL, Git";
                        prefSkills = "TensorFlow, PyTorch, Deep Learning, NLP, Pandas, NumPy, LangChain";
                        keywords = "Python, Machine Learning, ML, AI, TensorFlow, PyTorch, Data Science, SQL";
                        description = "As an AI/ML Engineer, you will research, build, and deploy machine learning models. You will work on data processing pipelines, train neural networks, and integrate AI models into our production software.";
                        responsibilities = "- Clean, preprocess, and analyze large datasets for training.\n- Train, tune, and evaluate machine learning/deep learning models.\n- Deploy ML models as web services or microservices.\n- Stay up-to-date with latest advancements in generative AI and LLMs.";
                    } else if (category == 5) { // Cloud / DevOps / SRE / Network / Security
                        reqSkills = "AWS, Linux, Docker, Bash scripting, Git";
                        prefSkills = "Kubernetes, Terraform, Jenkins, CI/CD, Azure, Cybersecurity, Networking";
                        keywords = "AWS, Cloud, DevOps, Linux, Docker, Kubernetes, CI/CD, Terraform, Security";
                        description = "We are looking for a DevOps/Cloud Engineer to manage cloud infrastructure, automate deployment pipelines, and ensure maximum application uptime. You will focus on cloud architecture, scaling, and security.";
                        responsibilities = "- Automate infrastructure provisioning using Terraform and shell scripts.\n- Set up and maintain CI/CD pipelines for automatic software delivery.\n- Monitor server performance, log files, and alert systems.\n- Implement cloud security controls, IAM roles, and network security policies.";
                    } else if (category == 6) { // Mobile Dev (Android / iOS / Flutter)
                        reqSkills = "Kotlin, Android Studio, Flutter, Dart, Git";
                        prefSkills = "Java, Swift, iOS, Firebase, REST APIs, Mobile UI design";
                        keywords = "Kotlin, Android, Flutter, Dart, Mobile, Firebase, Git, iOS";
                        description = "Join our mobile engineering team to build native and cross-platform applications. You will create highly performant mobile layouts, implement local storage, and consume REST APIs.";
                        responsibilities = "- Build and maintain mobile applications using Flutter or native Android SDK.\n- Integrate backend APIs, Push Notifications, and Firebase services.\n- Write clean, performance-optimized code for mobile devices.\n- Publish and update apps on Google Play Store and Apple App Store.";
                    } else { // Special Tech (Embedded, IoT, Blockchain, SAP, Salesforce, Game)
                        reqSkills = "C++, C, Linux, Unity, Salesforce, Apex, SAP";
                        prefSkills = "Solidity, Web3, IoT, RTOS, Microcontrollers, C#, 3D Math";
                        keywords = "C++, Embedded, IoT, Unity, Blockchain, Solidity, Salesforce, Apex, SAP";
                        description = "This role covers specialty areas such as embedded systems, blockchain smart contracts, Salesforce customization, or game development. You will work on targeted core features in your domain.";
                        responsibilities = "- Write embedded firmware or configure enterprise SAP/Salesforce solutions.\n- Design and develop interactive game scenes or write secure Solidity smart contracts.\n- Test hardware integrations or verify system compliance in sandbox environments.\n- Document technical designs and architecture flows.";
                    }

                    Job job = new Job();
                    job.setTitle(title);
                    job.setCompany(company);
                    job.setCompanyLogo(logoUrl);
                    job.setCompanyDescription(compDesc);
                    job.setLocation(location);
                    job.setWorkMode(workMode);
                    job.setExperienceRequired(experience);
                    job.setSalary(salary);
                    job.setRequiredSkills(reqSkills);
                    job.setPreferredSkills(prefSkills);
                    job.setKeywords(keywords);
                    job.setDescription(description);
                    job.setResponsibilities(responsibilities);
                    job.setQualifications(qualifications);
                    job.setRequirements(responsibilities); // Map requirements to responsibilities or description
                    job.setEmploymentType(employmentType);
                    job.setCreatedAt(LocalDateTime.now().minusDays(i % 10)); // Seed staggered posted dates

                    jobsToSave.add(job);
                }

                jobRepository.saveAll(jobsToSave);
                System.out.println("Successfully seeded " + jobsToSave.size() + " CSE/IT jobs into database.");
            }
        };
    }
}
