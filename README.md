# Knights Templar Alliance Dashboard

This application provides a comprehensive dashboard for the Knights Templar alliance in the game Politics & War. The dashboard includes alliance analysis, nation optimizations, resource tracking, MMR compliance checking, and optimal city build recommendations.

## Features

- **Automatic Data Refresh**: Automatically analyzes the Knights Templar alliance every hour
- **Interactive Dashboard**: Visualizes alliance data with charts and tables
- **Nation Analysis**: Break down of each nation with specific optimization recommendations
- **City Tier Analysis**: Compare nations by city count tiers
- **Military Tracking**: Track military units and MMR compliance
- **Resource Production**: Analyze resource production across the alliance
- **Optimal City Build**: Generate and export optimal city builds for any infrastructure level
- **One-Click Optimization**: Quickly get city build templates for in-game use
- **Dark Mode Support**: Toggle between light and dark themes

## Installation

1. Clone this repository:
```
git clone https://github.com/your-username/knights-templar-dashboard.git
cd knights-templar-dashboard
```

2. Install the required dependencies:
```
pip install flask requests matplotlib
```

3. Set up your P&W API key in the settings page or directly in `config.ini`:
```
[API]
key = YOUR_API_KEY
```

## Usage

1. Start the web application:
```
python run_flask_server.py
```

2. Open a web browser and navigate to:
```
http://localhost:5000
```

3. The dashboard will automatically run an analysis for Knights Templar (alliance #4124)

4. To generate an optimal build template, click the "Generate Optimal Build" button

5. To quickly get a city build template, click the "One-Click Optimal" button

## Project Structure

- `updated_pnw_data_analyzer.py` - The main analyzer script for fetching and processing alliance data
- `flask_app.py` - The Flask web application
- `run_flask_server.py` - Script to start the web server
- `static/` - Contains CSS and JavaScript files
- `templates/` - Contains HTML templates
- `output/` - Directory where generated reports are stored

## MMR Types

The dashboard supports different Military Minimum Requirements (MMR) types:

- **Peacetime MMR (0/2/5/0)**: Default for 20+ city nations
- **Raid MMR (5/0/0/0)**: Default for nations under 20 cities
- **Wartime MMR (5/5/5/3)**: For alliance-wide war preparation

## Requirements

- Python 3.6+
- Flask
- Requests
- Politics & War API key
- Web Browser

## License

This project is licensed under the MIT License.

## Acknowledgments

- Politics & War API for providing the data
- Knights Templar alliance for the community