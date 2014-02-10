SELECT a.simId, a.time, 
(SELECT COUNT(*) FROM `droolsSnapshot` WHERE simId = a.simId AND object LIKE CONCAT('provision(%, Measured%) at %', a.time)) AS provisions,
(SELECT COUNT(*) FROM `droolsSnapshot` WHERE simId = a.simId AND object LIKE CONCAT('appropriate(%, Measured%) at %', a.time)) AS appropriations,
(SELECT COUNT(*) FROM `droolsSnapshot` WHERE simId = a.simId AND object LIKE CONCAT('request(%type(Measured)%) at %', a.time)) AS requests
FROM gameActions AS a
GROUP BY a.simId, a.time
