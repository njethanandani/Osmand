class AddObdColumnsToPoint < ActiveRecord::Migration
  def change
    add_column :points, :rpm, :decimal, :precision => 10, :scale => 2
    add_column :points, :mileage, :decimal, :precision => 10, :scale => 2
    add_column :points, :fuel_consumed, :decimal, :precision => 10, :scale => 2
    add_column :points, :distance, :integer
    add_column :points, :type, :string
    add_column :points, :trouble_codes, :string
  end
end
