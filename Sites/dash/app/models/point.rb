class Point < ActiveRecord::Base
  belongs_to :car
  attr_accessible :elevation, :hdop, :lat, :lon, :name, :speed, :time, :rpm, :mileage, :fuel_consumed, :distance, :type, :trouble_codes
  acts_as_gmappable :process_geocoding => false

  def gmaps4rails_address
    "#{lat}, #{lon}"
  end
  
  def latitude
    lat
  end

  def longitude
    lon
  end
end
