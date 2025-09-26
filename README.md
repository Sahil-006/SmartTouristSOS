Smart Tourist Safety Monitoring & Incident Response System


Smart India Hackathon 2025 - Problem Statement ID: 25002

🎯 Project Vision
An integrated, privacy-first platform that issues temporary blockchain-backed Digital Tourist IDs, monitors tourist safety with geo-fencing & AI, and automates incident response for tourism and police authorities.

Our goal is to create a safer, stress-free travel experience for tourists while providing authorities with data-driven tools for faster and more effective incident response.

✨ Key Features of the Full System
🔒 Digital Tourist ID: Secure, temporary, and tamper-proof digital identities for tourists, backed by a blockchain for ultimate trust and privacy.

🗺️ Geo-fencing & AI Safety: Intelligent monitoring that detects unusual tourist movements, alerts users about unsafe zones, and identifies potential distress signals (like inactivity or itinerary deviation).

🤝 Group Safety Connect: Family or group members can link their IDs. If one person strays more than a set distance away, all members receive an immediate in-app and email alert with a live location link.

🚨 Automated Incident Response: Authorities receive instant, actionable alerts with the tourist's verified ID, last-known location, and emergency contacts.

📊 Authority Dashboard: A central dashboard for police and tourism departments featuring heatmaps of tourist activity, incident analytics, and feedback insights to improve safety planning.

🌐 Global Feedback System: A transparent platform for tourists to share reviews and feedback, building trust and accountability within the tourism ecosystem.

📱 About This Repository: The Android SOS App Prototype
This repository contains the code for the Android SOS Application, which serves as a crucial proof-of-concept for the "Incident Response" module of our larger system. This app demonstrates the core emergency trigger mechanism.

Features Implemented in this Prototype:
Emergency Contact Selection: Users can pick an emergency contact from their phone's contact list.

Background Service: A persistent foreground service listens for the trigger even when the app is closed.

Power Button Trigger: Pressing the power button 3 times within 5 seconds triggers the SOS alert.

GPS Location Fetching: Instantly fetches the device's current latitude and longitude.

Automatic SMS Alert: Sends an SMS to the saved contact with a custom message and a direct Google Maps link.

🛠️ Proposed Tech Stack (Full Project)
Category

Technology

Backend :-

Python (Flask / Django)

Frontend :-

React (for the Authority Dashboard)

Mobile App :-

Kotlin (for the Android Tourist App)

Database :-

MongoDB / PostgreSQL

Blockchain :-

Hyperledger Fabric

AI/ML :-

TensorFlow / PyTorch (for behavior analysis)

Cloud :-

AWS (EC2, S3, RDS)

DevOps :-

Docker, GitHub Actions

📋 How to Run This Android Prototype
Clone the Repository: Use the "Get from VCS" option in Android Studio.

Connect a Device: Connect a real Android device with a SIM card and SMS plan.

Build & Run: Run the app from Android Studio.

Grant Permissions: Allow all requested permissions (Contacts, Notifications, Location, SMS).

Set Contact: Use the "Pick Emergency Contact" button to save a number.

Enable Service: Tap "Enable SOS Listener".

Test: Press the power button 3 times to trigger the SMS alert.
