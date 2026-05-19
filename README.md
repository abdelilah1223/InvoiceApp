# Invoice Application

A comprehensive Android invoice management application built with Kotlin.

## Features

### Current Features
- Create and manage professional invoices
- Add company and client information
- Add multiple items with quantities and prices
- Automatic calculation of totals, taxes, and subtotals
- Save invoices to local database
- Export invoices in multiple formats (PDF, JSON, CSV, Text)
- Generate QR codes for invoices
- Share invoices via QR codes or other apps
- Arabic and English language support
- Customizable background colors
- Modern, responsive UI with animations
- Ad-supported free application

### Planned Features
- Cloud synchronization across devices
- Email invoices directly from the application
- Customizable invoice templates
- Generate reports and analytics
- Multi-currency support
- Integration with payment gateways
- Automatic invoice reminders
- Inventory management system
- Multi-user support with permissions

## Project Architecture

This Android application follows a modern architecture pattern using Fragments and Activities:

### Main Components
- **SplashActivity**: Initial loading screen with animations
- **MainActivity**: Main container activity that hosts all fragments
- **HomeFragment**: Displays list of saved invoices with options to view, edit, export
- **AddFragment**: Form for creating and editing invoices
- **ServicesFragment**: Contact options and social media links
- **SettingsFragment**: Application settings for language and theme

### Data Layer
- **SQLite Database**: Local storage for invoices using a custom SQLiteOpenHelper
- **Gson**: JSON serialization for storing invoice items
- **Content Providers**: For sharing files externally

### Key Libraries
- Android Jetpack Components (Fragments, RecyclerView, etc.)
- Google Gson for JSON serialization
- iText 7 for PDF generation
- ZXing for QR code generation
- Google AdMob for advertisements
- Material Design Components for UI

### Export Functionality
- PDF generation with HTML templating
- JSON, CSV, and plain text export options
- QR code generation and sharing capabilities

## Installation

### Download
You can download the latest version of the application directly from:
[abdelilah.kesug.com/archive/apps](http://abdelilah.kesug.com/archive/apps)

### Clone Repository
To clone the repository, use:
https://github.com/abdelilah1223/InvoiceApp.git
