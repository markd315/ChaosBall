export host=zanzalaz.com
status=69
while (($status == 69))
do
	java -cp Chaosball.jar client.ChaosballWindow
	status=$?
done