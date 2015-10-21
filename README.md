# Knowledge Commons simulation

This project contains code to simulate a Knowledge Commons, as described in Sam Macbeth's [Thesis](https://spiral.imperial.ac.uk:8443/bitstream/10044/1/25751/1/Macbeth-S-2015-PhD-Thesis.pdf). Simulations use the [Presage2](http://www.presage2.info) simulation platform.

### Prerequisites

You will need Java installed (version 6 or greater), as well as the [maven](http://maven.apache.org/) dependency management tool. Saving simulation data requires a [MySQL](https://mariadb.org/) database.

### Drools EInst

The simulations use the [Drools EInst](https://github.com/sammacbeth/electronic-institutions) library in order to reason about institutionalised power. You will need this library installed in your local maven repository.

If you use [Eclipse](https://www.eclipse.org/), then simply downloading the Drools EInst project and adding it as a maven project in Eclipse is sufficient to satisfy the dependency.

Otherwise, the following command-line instructions can be used:

```bash
# checkout Drools-EInst from github
git clone https://github.com/sammacbeth/electronic-institutions.git
# move into project directory
cd electronic-institutions
# compile and install to local repository
mvn install
```

### Download and compile

```bash
# checkout KnowledgeCommons from github
git clone https://github.com/sammacbeth/KnowledgeCommons.git
# move into project directory
cd KnowledgeCommons
# compile and test
mvn verify
```

### Database configuration

Database configuration should be placed in the `src/main/resources/db.properties` file as follows:

```
module=uk.ac.imperial.presage2.db.sql.SqlModule
implementation=kc.util.KCStorage
url=jdbc:mysql://localhost/knowledgecommons
driver=com.mysql.jdbc.Driver
user=database_user
password=database_password
```

Values for `url`, `user` and `password` should be replaced with appropriate values for your database configuration. The database should already be created, and the user account specified should have `CREATE TABLE` privileges, as well as the usual `INSERT`, `UPDATE` and `DELETE`.

### Command line invocation

The primary method to add and run simulations is via the command line interface. The `kccli` file provides access to various cli commands:

```
$ ./kccli --help
usage: presage2-cli <command> [OPTIONS]
 add         Add a new simulation.
 duplicate   Duplicate a simulation.
 insert      Insert a batch of simulations to run.
 list        Lists all simulations
 run         Run a simulation.
 runall      Run all simulations which have not yet started
 shell       Open an interactive session
```

We can now test our database connection, using the `./kccli list` command to verify we can retrieve a list of simulations from MySQL. Should this command give an exception, then likely there is a problem with the database configuration.

### Adding experiment specifications

Under the `insert` command, groups of simulations are ordered into 'experiments'. Each experiment specifies a parameter space to test. The parameter spaces are defined in `src/main/kc/util/KCCLI.java`.

```bash
$ ./kccli insert --help
usage: presage2cli insert <experiment>
 bandits         Get properties of the bandit game.
 building
 facilities
 facilitysub
 power
 pseudo          Get properties of the pseudo game.
 selforg
 static
 subcollective
 supply
```

Every experiment also takes `repeats` and `seed` options, to specify how many repeats of each parameter permutation should be made, and what random seed should be given to the simulation (this seed is incremented for repeats).

### Running simulations

The insert command simply inserts the specifications for each simulation into the database, but does not run any. To run simulations we require the `run` and `runall` commands.

`kccli run` enables a single specified simulation to be run, using the simulation ID to specify which one. Simulation IDs can be found with the `kccli list` command, or by looking in the 'simulations' table in the database.

`kccli runall` automates the running of a batch of simulations consecutively, or in parallel. By default simulations will be run serially on the local machine. More advanced configurations can be used to run multiple simulations simulataneously on both local and remote machines. Such configuration can be done in the `src/main/java/resources/executors.json` file, following the format described [here](http://presage.github.io/docs/reference/uk/ac/imperial/presage2/core/cli/run/ExecutorModule.html#load%28%29) (example [here](https://github.com/Presage/eclipse-quickstart/blob/master/src/main/resources/executors.json))
