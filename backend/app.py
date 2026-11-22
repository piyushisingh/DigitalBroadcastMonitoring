import os
from flask import Flask, jsonify, request, g
from database import close_db, init_db, DEFAULT_DB_PATH
from routes.health_routes import health_bp
from routes.program_routes import program_bp
from routes.schedule_routes import schedule_bp
from routes.dashboard_routes import dashboard_bp
from routes.report_routes import report_bp
from routes.alert_routes import alert_bp
from routes.ml_routes import ml_bp


def create_app(test_config=None):
    """
    Application factory for Digital Broadcast Monitoring REST API.
    Configures database teardown, registers blueprints, and sets up JSON error handling.
    """
    app = Flask(__name__, instance_relative_config=True)

    # Default configuration
    app.config.from_mapping(
        DATABASE=DEFAULT_DB_PATH,
        TESTING=False,
    )

    if test_config is not None:
        app.config.update(test_config)

    # Set g.db_path per request so database.py connects to the configured database
    @app.before_request
    def set_db_context():
        g.db_path = app.config.get("DATABASE")

    # CORS support for Android emulator (10.0.2.2) and local debugging
    @app.after_request
    def add_cors_headers(response):
        response.headers["Access-Control-Allow-Origin"] = "*"
        response.headers["Access-Control-Allow-Methods"] = "GET, POST, PUT, DELETE, OPTIONS"
        response.headers["Access-Control-Allow-Headers"] = "Content-Type, Authorization"
        return response

    # Global options handler
    @app.route("/", defaults={"path": ""}, methods=["OPTIONS"])
    @app.route("/<path:path>", methods=["OPTIONS"])
    def options_handler(path):
        return "", 204

    # Register Blueprints
    app.register_blueprint(health_bp)
    app.register_blueprint(program_bp)
    app.register_blueprint(schedule_bp)
    app.register_blueprint(dashboard_bp)
    app.register_blueprint(report_bp)
    app.register_blueprint(alert_bp)
    app.register_blueprint(ml_bp)

    # Teardown database connection
    app.teardown_appcontext(close_db)

    # Root informational route
    @app.route("/", methods=["GET"])
    def root_info():
        return jsonify({
            "service": "Digital Broadcast Monitoring API",
            "version": "1.0.0",
            "documentation": "/api/health",
            "endpoints": [
                "/api/health",
                "/api/ml/health",
                "/api/ml/stalling-risk",
                "/api/programs",
                "/api/programs/<id>",
                "/api/schedules",
                "/api/schedules/<id>",
                "/api/schedules/<id>/analysis",
                "/api/dashboard",
                "/api/reports",
                "/api/alerts"
            ]
        }), 200

    # JSON Error Handlers
    @app.errorhandler(400)
    def bad_request(e):
        return jsonify({"error": "Bad request", "details": str(e)}), 400

    @app.errorhandler(404)
    def not_found(e):
        return jsonify({"error": "Resource not found", "path": request.path}), 404

    @app.errorhandler(405)
    def method_not_allowed(e):
        return jsonify({"error": "Method not allowed for this endpoint"}), 405

    @app.errorhandler(500)
    def internal_error(e):
        return jsonify({"error": "Internal server error", "details": str(e)}), 500

    return app


if __name__ == "__main__":
    app = create_app()
    port = int(os.environ.get("PORT", 5000))
    print(f"Starting Digital Broadcast Monitoring Flask server on 0.0.0.0:{port}...")
    print(f"Android Emulator base URL: http://10.0.2.2:{port}")
    app.run(host="0.0.0.0", port=port, debug=True)
