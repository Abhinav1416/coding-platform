local key = KEYS[1]
local userHandle = ARGV[1]
local problemId = ARGV[2]
local verdict = ARGV[3]
local timeTaken = tonumber(ARGV[4])

local PENALTY_PER_WRONG_ATTEMPT = 600

local rawData = redis.call('GET', key)
if not rawData then return nil end
local data = cjson.decode(rawData)

if not data['scoreboard'] then data['scoreboard'] = {} end
if not data['scoreboard']['users'] then data['scoreboard']['users'] = {} end

if not data['scoreboard']['users'][userHandle] then
    data['scoreboard']['users'][userHandle] = {
        solved = 0,
        penalty = 0,
        problems = {}
    }
end

local userStats = data['scoreboard']['users'][userHandle]
local problems = userStats['problems']

if not problems[problemId] then
    problems[problemId] = {
        status = "NONE",
        attempts = 0,
        time = 0
    }
end

local problemStats = problems[problemId]

if problemStats['status'] == "OK" then
    return cjson.encode(data)
end

if verdict == "OK" then
    problemStats['status'] = "OK"
    problemStats['attempts'] = problemStats['attempts'] + 1
    problemStats['time'] = timeTaken
    userStats['solved'] = userStats['solved'] + 1
    local wrongAttempts = problemStats['attempts'] - 1
    local extraPenalty = wrongAttempts * PENALTY_PER_WRONG_ATTEMPT

    userStats['penalty'] = userStats['penalty'] + timeTaken + extraPenalty

else
    problemStats['status'] = verdict
    problemStats['attempts'] = problemStats['attempts'] + 1
end

local encoded = cjson.encode(data)
redis.call('SET', key, encoded, 'KEEPTTL')

return encoded