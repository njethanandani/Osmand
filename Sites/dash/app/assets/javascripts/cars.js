function add_points_to_map(points){
    for(var i=0;i<  points.length; i++)
    	L.marker([points[i][0], points[i][1]]).addTo(map);
}
function bound_map_to_points(points){
	lat_long_bound = create_latlng_bound(points);
	map.fitBounds(lat_long_bound);
}
function draw_route(points){
	var linePts = new Array();
	for( i=0; i < points.length; i++ ) 
  		linePts[ i ] = new L.LatLng( points[ i ][ 0 ], points[ i ][ 1 ] );
	var polyline = L.polyline(linePts, {color: 'purple',fillOpacity: 1.0}).addTo(map);
	map.addLayer(polyline);
}
function create_latlng_bound(points){
	lat_long_bound = new L.latLngBounds(points);
	return lat_long_bound;
}
