local key = KEYS[1]
local userHandle = ARGV[1]
local problemId = ARGV[2]
local verdict = ARGV[3]
local currentTimestamp = tonumber(ARGV[4])
local execTime = tonumber(ARGV[5]) or 0
local memory = tonumber(ARGV[6]) or 0

local PENALTY_PER_WRONG_ATTEMPT = 1200

local rawData = redis.call('GET', key)
if not rawData then return nil end
local data = cjson.decode(rawData)

local startTime = tonumber(data['startTime']) or currentTimestamp
local relativeTime = currentTimestamp - startTime
if relativeTime < 0 then relativeTime = 0 end

if not data['scoreboard'] then data['scoreboard'] = {} end
if not data['scoreboard']['users'] then
    data['scoreboard']['users'] = { ['@class'] = 'java.util.HashMap' }
end

if not data['scoreboard']['users'][userHandle] then
    data['scoreboard']['users'][userHandle] = {
        ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$DuelUserStats',
        solved = 0,
        penalty = 0,
        problems = { ['@class'] = 'java.util.HashMap' }
    }
end

local userStats = data['scoreboard']['users'][userHandle]
local problems = userStats['problems']

if not problems[problemId] then
    problems[problemId] = {
        ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$ProblemStats',
        status = "NONE",
        attempts = 0,
        bestTime = 0,
        history = { ['@class'] = 'java.util.LinkedHashMap' }
    }
end

local problemStats = problems[problemId]

local newSubmission = {
    ['@class'] = 'com.Abhinav.backend.features.duel.model.DuelScoreboard$SubmissionData',
    verdict = verdict,
    timeConsumedMillis = execTime,
    memoryConsumedBytes = memory,
    submissionTimeSeconds = relativeTime
}

local count = 0
if problemStats['history'] then
    for _ in pairs(problemStats['history']) do count = count + 1 end
    if problemStats['history']['@class'] then count = count - 1 end
end
local historyKey = tostring(count + 1)

problemStats['history'][historyKey] = newSubmission

if problemStats['status'] ~= "OK" then
    if verdict == "OK" then
        problemStats['status'] = "OK"
        problemStats['attempts'] = problemStats['attempts'] + 1
        problemStats['bestTime'] = relativeTime

        userStats['solved'] = userStats['solved'] + 1
        local wrongAttempts = problemStats['attempts'] - 1
        local extraPenalty = wrongAttempts * PENALTY_PER_WRONG_ATTEMPT
        userStats['penalty'] = userStats['penalty'] + relativeTime + extraPenalty
    else
        if verdict ~= "COMPILATION_ERROR" then
             problemStats['attempts'] = problemStats['attempts'] + 1
        end
    end
end

local encoded = cjson.encode(data)
redis.call('SET', key, encoded, 'KEEPTTL')

return encoded