
/**
 * Game Service - Invoke MW API's, transform data for UI and viceversa
 *
 * @author rayulu
 */
var async = require('async')
	, mwService = require('../commons/MWServiceProvider')
	, util = require('../commons/Util')
	, urlConstants = require('../commons/URLConstants')
	, _ = require('underscore');

exports.getGameCoverage = function(tid, cb) {
	async.parallel({
		games: function(callback) {
			var args = {
				parameters: {
					taxonomyId: tid,
					objectType: 'Game'
				}
			}
			mwService.getCall(urlConstants.GET_GAMES, args, callback);
		},
		concepts: function(callback) {
			var args = {
				parameters: {
					taxonomyId: tid,
					games: true
				}
			}
			mwService.getCall(urlConstants.GET_CONCEPTS, args, callback);
		}
	}, function(err, results) {
		if(!util.validateMWResponse(results.games, cb)) {
			return;
		}
		if(!util.validateMWResponse(results.concepts, cb)) {
			return;
		}
		
		var data = {
			concepts: _.pluck(results.concepts.result.RESULTS.valueObjectList, 'baseValueMap'),
			games: [],
			rowLabel: [],
			colLabel: [],
			matrix: [],
			stats: {
				noOfGames: 0,
				noOfConcepts: 0,
				conceptsWithNoGame: 0,
				conceptsWithNoScreener: 0
			}
		};
		_.each(results.games.result.LEARNING_OBJECTS.valueObjectList, function(game) {
			data.games.push({identifier: game.identifier, name: game.metadata.name, purpose: game.metadata.purpose});
			data.colLabel.push({id: game.identifier, name: game.metadata.name});
		});
		data.colUniqueIds = _.pluck(data.colLabel, 'id');
		data.rowUniqueIds = _.pluck(data.concepts, 'id');
		data.stats.noOfGames = data.games.length;
		data.stats.noOfConcepts = data.concepts.length;
		var gameMap = {};
		_.each(data.games, function(game) {
			game.conceptCount = 0;
			gameMap[game.identifier] = game;
		});
		_.each(data.concepts, function(concept) {
			data.rowLabel.push({id: concept.id, name: concept.name});
			var conceptGames = _.pluck(concept.games, 'identifier');
			var gameCount = _.where(concept.games, {purpose: 'Game'}).length;
			var screenerCount = _.where(concept.games, {purpose: 'Screener'}).length;
			concept.gameCount = concept.games ? concept.games.length : 0;
			if(gameCount == 0) {
				data.stats.conceptsWithNoGame++;
			}
			if(screenerCount == 0) {
				data.stats.conceptsWithNoScreener++;
			}
			_.each(concept.games, function(game) {
				gameMap[game.identifier].conceptCount++;
			});
			_.each(data.games, function(game) {
				data.matrix.push({
					row: data.rowUniqueIds.indexOf(concept.id) + 1,
					rowId: concept.id,
					colId: game.identifier,
					col: data.colUniqueIds.indexOf(game.identifier) + 1,
					value: (conceptGames.indexOf(game.identifier) == -1 ? 0 : (game.purpose == 'Game' ? 1 : 2))
				});
			});
		});
		cb(null, data);
	});
}

exports.getGameDefinition = function(cb, taxonomyId) {
	var args = {
		path: {
			id: taxonomyId
		}
	}
	mwService.getCall(urlConstants.GET_GAME_TAXONOMY_DEFS, args, function(err, data) {
		if(err) {
			cb(err);
		} else {
			cb(null, data.result.DEFINITION_NODE);
		}
	});
}

exports.getGames = function(cb, taxonomyId, offset, limit) {
	var args = {
		parameters: {
			taxonomyId: taxonomyId,
			objectType: 'Game',
			offset: offset,
			limit: limit
		}
	}
	mwService.getCall(urlConstants.GET_GAMES, args, function(err, data) {
		if(err) {
			cb(err);
		} else {
			var games = data.result.LEARNING_OBJECTS.valueObjectList;
			var count = data.result.COUNT.id;
			var result = {};
			result.games = games;
			result.count = count;
			cb(null, result);
		}
	});
}

exports.getGame = function(cb, taxonomyId, gameId) {
	async.parallel({
		game: function(callback) {
			var args = {
				path: {id: gameId},
				parameters: {taxonomyId: taxonomyId}
			}
			mwService.getCall(urlConstants.GET_GAME, args, callback);
		},
		auditHistory: function(callback) {
			callback(null, []);
		},
		comments: function(callback) {
			callback(null, []);
		}
	}, function(err, results) {
		if(err) {
			cb(err);
		} else {
			var game = results.game.result.LEARNING_OBJECT;
			game.auditHistory = results.auditHistory;
			game.comments = results.comments;
			cb(null, game);
		}
	});
}

exports.updateGame = function(data, cb) {
  	var args = {
		path: {id: data.identifier, tid: data.taxonomyId},
		data: {
			request: {
				LEARNING_OBJECT: {
					identifier: data.identifier,
	        		objectType: "Game",
	        		metadata: data.properties,
	        		tags: data.tags
				},
				METADATA_DEFINITIONS: []
			},
			COMMENT: data.comment
		}
	}
	if(data.newMetadata && data.newMetadata.length > 0) {
		_.each(data.newMetadata, function(prop) {
			args.data.request.METADATA_DEFINITIONS.push(_.omit(prop, 'error'));
		});
	}
	mwService.patchCall(urlConstants.UPDATE_GAME, args, function(err, data) {
		if(err) {
			cb(err);
		} else if(util.validateMWResponse(data, cb)) {
			cb(null, 'OK');
		}
	});
}

exports.createGame = function(data, cb) {
  	var args = {
		path: {tid: data.taxonomyId},
		data: {
			request: {
				LEARNING_OBJECT: {
	        		objectType: "Game",
	        		metadata: {
	 					"name": data.name,
	 					"code": data.code,
	 					"appIcon": data.appIcon,
	 					"posterImage": data.posterImage,
	 					"owner": data.owner,
	 					"developer": data.developer,
	 					"description": data.description
					}
				}
			},
			COMMENT: data.comment
		}
	}
	mwService.postCall(urlConstants.SAVE_GAME, args, function(err, data) {
		if(err) {
			cb(err);
		} else if(util.validateMWResponse(data, cb)) {
			cb(null, data.result.NODE_ID);
		}
	});
}
