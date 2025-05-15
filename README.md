# Knights Templar Alliance Dashboard

This application provides a comprehensive dashboard for the Knights Templar alliance in the game Politics & War. The dashboard includes alliance analysis, nation optimizations, resource tracking, MMR compliance checking, and optimal city build recommendations.

## Features

- **API and Resource Caching** Saves user settings and caches API-pulled data for efficiency
- **Interactive Dashboard**: Visualizes alliance data with charts and tables
- **Nation Analysis**: Break down of each nation with specific optimization recommendations
- **City Tier Analysis**: Compare nations by city count tiers, with graphical implementations
- **MMR Tracking**: Track military units and MMR compliance
- **Resource Production**: Analyze resource production across the alliance
- **Optimal City Build**: Generate and export optimal city builds for any infrastructure level
- **One-Click Optimization**: Quickly get city build templates for in-game use
- **Dark Mode Support**: Toggle between light and dark themes

## Installation

1. Clone this repository:
```
git clone https://github.com/nicholaslawrence-hub/citadelbot)
cd citadelbot
```

2. Install Java:

Can be found on Oracle, Java SDK 17+ required. 

3. Set up your P&W API key in the settings page or directly in `config.ini`:
4. 
```
[API]
key = YOUR_API_KEY
```

## Usage

1. Run packaged jar file with Java
```
java -jar pnw-analyzer-1.0.0-SNAPSHOT.jar
```

2. Open a web browser and navigate to:
```
http://localhost:5000
```

3. The dashboard will open, and actions can be taken on the GUI to go through with analyses

4. To generate an optimal build template, click the "Generate Optimal Build" button

5. To quickly get a city build template, click the "One-Click Optimal" button

## Project Structure

## MMR Types

The dashboard supports different Military Minimum Requirements (MMR) types:

- **Peacetime MMR (0/2/5/0)**: Default for 20+ city nations
- **Raid MMR (5/0/0/0)**: Default for nations under 20 cities
- **Wartime MMR (5/5/5/3)**: For alliance-wide war preparation

## Requirements

- Java (SDK 17+)
- Politics & War API key
- Web Browser

## License

This project is licensed under the MIT License.

## Acknowledgments
