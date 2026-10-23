-- RedCircuit + CC: Tweaked - przykład
-- Kabel wiązkowy RedCircuit podłączony do lewej strony komputera.

local side = "left"

-- 1) Wyjście: zapal kanały biały (1) i czerwony (colors.red) na kablu
redstone.setBundledOutput(side, colors.combine(colors.white, colors.red))

-- 2) Wejście: który kanał przychodzi z kabla?
local input = redstone.getBundledInput(side)
for name, c in pairs(colors) do
  if type(c) == "number" and colors.test(input, c) then print("aktywny kanal: " .. name) end
end

-- 3) Bramka-timer obok (peryferium "redcircuit_gate")
local gate = peripheral.find("redcircuit_gate")
if gate then
  print("bramka: " .. gate.getGateType())
  if gate.getGateType() == "timer" then gate.setInterval(40) end   -- 2 sekundy
  if gate.getGateType() == "counter" then gate.setMax(8); gate.resetCount() end
end

-- 4) Nadajnik/odbiornik bezprzewodowy (peryferium "redcircuit_wireless")
local radio = peripheral.find("redcircuit_wireless")
if radio then
  radio.setFrequency(42)
  print("kanal 42 aktywny: " .. tostring(radio.isChannelActive()))
end

-- 5) Czekanie na zmianę sygnału z kabla
while true do
  os.pullEvent("redstone")
  print("nowe wejscie: " .. redstone.getBundledInput(side))
end
