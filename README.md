# myRSync

This simple Application is intended to be a convenient way to create and schedule an rsync command in order to synchronize local folders with a server that is running an rsync daemon.

It is an ideal solution for people that need to implement a custom cloud file backup in their network so to have total control over where their files are stored.

<h3>Description</h3>

This application consists of four tabs:
1. Overview - As long as there are active Schedulers, In Overview tab the user can get quick information about the Configurations that are Scheduled to run, the next running time, the last result etc.
2. Configurations - This tab shows a list with the Users Configurations. There is also a Floating Button that is used to add create and save a configuration.
3. Schedulers - As long as there is at least one saved configuration, the user can create Schedulers and attach them to the configurations. These will ensure that the configuration will run according to the Scheduler.
4. Log - A tab that is showing log entries of the rsync command. 


<h3>How to use this app:</h3>

1. Create Rsync Configurations.
2. Run them on demand, or create Schedulers attached to them.
3. Check the Overview for details regarding each scheduler/configuration pair.
4. Check the Log for the rsync log entries.

Google Playstore Beta Download: <a href="https://play.google.com/store/apps/details?id=com.linminitools.myrsync">https://play.google.com/store/apps/details?id=com.linminitools.myrsync</a>
